package rs.ac.uns.ftn.informatika.rest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.informatika.rest.model.Terapija;

@Repository
public interface TerapijaRepository extends JpaRepository<Terapija, Long> {
}
