package rs.ac.uns.ftn.informatika.rest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.informatika.rest.dto.DateTimeDTO;
import rs.ac.uns.ftn.informatika.rest.dto.RezervacijaDTO;
import rs.ac.uns.ftn.informatika.rest.dto.RezervacijaWithFlagDTO;
import rs.ac.uns.ftn.informatika.rest.model.Apoteka;
import rs.ac.uns.ftn.informatika.rest.model.Korisnik;
import rs.ac.uns.ftn.informatika.rest.model.Lek;
import rs.ac.uns.ftn.informatika.rest.model.Rezervacija;
import rs.ac.uns.ftn.informatika.rest.repository.RezervacijaRepository;

import javax.validation.constraints.Email;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class RezervacijaService {

    @Autowired
    private RezervacijaRepository rezervacijaRepository;

    @Autowired
    private ApotekaService apotekaService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private KorisnikService korisnikService;


    public Rezervacija findRezervacijaByID(Long id) {
        Rezervacija r = rezervacijaRepository.findByID(id);

        return r;
    }

    // nadji rezervaciju ako jos nije preuzet(tj nema datum za preuzimanje) i rok za preuzimanje nije kraci od 24h
    public Rezervacija findActiveRezervacijaByID(Long id, String pharmacistUsername) {
        Rezervacija r = rezervacijaRepository.findByID(id);

        if(r == null)
            return null;

        if(!isPharmacistWorkingThere(r.getApoteka().getNaziv(), pharmacistUsername)){
            return null;
        }

        if(r.getDatumPreuz() != null) {
            System.out.println("Rezervacija je vec preuzeta");
            return null;
        }

        if(!isCancellingDateMoreThenOneDay(r.getRokZaPreuzimanje())) {
            System.out.println("================== CANCELLING DATE IS LESS THEN 24 HOURS, sry");
            return null;
        }

        return r;
    }

    // proveri da li farmaceut radi u apoteci gde je rezervacija kreirana
    public boolean isPharmacistWorkingThere(String pharmacy, String pharmacistUsername) {
        Apoteka a = apotekaService.findApotekaByNaziv(pharmacy);

        for(Korisnik k : a.getZaposleni()) {
            if (k.getUsername().equals(pharmacistUsername))
                System.out.println("+++ farmaceut radi u toj apoteci");
                return true;
        }
        System.out.println("+++ farmaceut NE radi u toj apoteci");
        return false;
    }

    public boolean isCancellingDateMoreThenOneDay(Date cancelDate) {
        LocalDate date = createLocalDateFromString(cancelDate);
        LocalDateTime ldt = date.atTime(LocalTime.parse(cancelDate.toString().split(" ")[1]));
        LocalDateTime ldt_now = LocalDateTime.now();

        long ldt_in_seconds = ldt.toEpochSecond(ZoneOffset.UTC);
        long ldt_now_in_seconds = ldt_now.toEpochSecond(ZoneOffset.UTC);
        long seconds_in_24_hours = 24*60*60;
        System.out.println("INPUT <---> NOW");
        System.out.println(ldt + " <---> " + ldt_now);
        System.out.println(ldt_in_seconds - ldt_now_in_seconds);
        if (ldt_in_seconds - ldt_now_in_seconds < seconds_in_24_hours) {
            System.out.println("RETURNED FALSE");
            return false;
        }
        return true;
    }

    public LocalDate createLocalDateFromString(Date d) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String datumPosete = dateFormat.format(d);
        LocalDate ret = LocalDate.parse(datumPosete);
        return ret;
    }

    public Rezervacija issueReservation(String id) {
        Rezervacija r = rezervacijaRepository.findByID(Long.parseLong(id));

        if(!sendConfirmationMailToPatient(r.getPacijent().getUsername(), id, r.getPacijent().getEmail())) {
            return null;
        }

        r.setDatumPreuz(new Date());
        r = rezervacijaRepository.save(r);

        return r;
    }

    public boolean sendConfirmationMailToPatient(String username, String resId ,String email) {
        String msg = "Dear " + username + ", ";
        msg += "Your reservation with code: " + resId + " has been issued!";
        boolean flag = emailService.sendSimpleMessage(email, "Reservation issue CONFIRMATION", msg);
        return flag;
    }

    public List<RezervacijaWithFlagDTO> findAllActiveReservationsForUser(String username) {

        Long id = korisnikService.findByUsername(username).getID();
        List<Rezervacija> all = rezervacijaRepository.findAllByPacijentID(id);
        List<RezervacijaWithFlagDTO> filteredList = new ArrayList<>();
        for(Rezervacija r : all) {
            if(r.getDatumPreuz() == null && r.getRokZaPreuzimanje().after(new Date())) {
                filteredList.add(new RezervacijaWithFlagDTO(r, isCancellingDateMoreThenOneDay(r.getRokZaPreuzimanje())));
            }
        }
        return filteredList;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancelReservation(long id) {

        Rezervacija r = rezervacijaRepository.findByID(id);
        if (r == null)
            return false;

        Apoteka a = r.getApoteka();
        Lek l = r.getLek();
        int current_quantity = a.getMagacin().get(l.getID());
        a.getMagacin().put(l.getID(), current_quantity + 1);
        rezervacijaRepository.deleteRezervacijaByID(id);
        return true;
    }

    public boolean createReservation(RezervacijaDTO dto, String username) throws ParseException {

        Lek l = apotekaService.lekRepository.findLekByID(dto.getLek());
        Korisnik k = korisnikService.findByUsername(username);
        Apoteka a = apotekaService.findApotekaByNaziv(dto.getApoteka());
        DateTimeDTO dateTimeDTO = new DateTimeDTO();
        dateTimeDTO.setDate(dto.getRokZaPreuzimanje());
        if (l == null || k == null || a == null)
            return false;

        Rezervacija newRez = new Rezervacija();
        newRez.setRokZaPreuzimanje(dateTimeDTO.parseDateStringToDate());
        newRez.setApoteka(a);
        newRez.setLek(l);
        newRez.setPacijent(k);
        Rezervacija rez = rezervacijaRepository.save(newRez);
        emailService.sendReservationCreatedMessage(rez, dto.getRokZaPreuzimanje());
        Map<Long, Integer> map = a.getMagacin();
        a.getMagacin().put(dto.getLek(), map.get(dto.getLek()) - 1);
        apotekaService.saveApoteka(a);

        return true;
    }


    public boolean removeMedicineAllowed(Long id, Apoteka a) {
        List<Rezervacija> rezervaije = rezervacijaRepository.findRezervacijaByApotekaIDAndLEKID(a.getID(), id);
        for (Rezervacija r : rezervaije) {
            if (r.getRokZaPreuzimanje().after(new Date()) && (r.getDatumPreuz() == null || r.getDatumPreuz().after(new Date()))) {
                return false;
            }
        }
        return true;
    }

    public int updateReservationPenaltiesForUser(String username) {

        Korisnik k = korisnikService.findByUsername(username);

        int sum = 0;
        for(Rezervacija r : rezervacijaRepository.findAllByPacijentID(k.getID())) {

            if(r.isPenalized()) {
                continue;
            }
            // not penalized
            if(r.getDatumPreuz() != null) {
                continue;
            }
            if(r.getRokZaPreuzimanje().compareTo(new Date()) < 0) {
                r.setPenalized(true);
                rezervacijaRepository.save(r);
                sum += 1;
            }

        }

        if (sum == 0) return 0;

        int current = k.getLoyaltyInfo().getPenali();
        k.getLoyaltyInfo().setPenali(current + sum);
        korisnikService.userRepository.save(k);
        return sum;

    }
}
