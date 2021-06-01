var usernm

$(document).ready(function() {
    getMe();
})

function getMe() {
    $.ajax({
        type:'GET',
        url: '/phadmin/whoami',
        contentType : 'application/json',
        beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.getItem('myToken'));
        },
        success : function(phadmin) {
            if(phadmin.prvoLogovanje == true) {
                console.log('Prvi put je logovan.')
                window.location.href = 'index.html';
            }
            const params = new URLSearchParams(window.location.search)
            usernm = params.toString();
            usernm = usernm.slice(0, -1)

            $.ajax({
                type: 'GET',
                url: '/dermatologist/getdermwithradnoinfo/' + usernm,
                contentType: 'application/json',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.getItem('myToken'));
                },
                success : function(dermatolog) {
                    addPageData(dermatolog)
                }
            })
        }
    })
}

function addPageData(dermatolog) {
    $('#name').val(dermatolog.ime)
    $('#lastname').val(dermatolog.prezime)

    var table = "";
    dermatolog.radnaVremena.forEach(function(rv) {
        table +=    `<tr>
                        <td>${rv.odDatum}</td>
                        <td>${rv.doDatum}</td>
                        <td>${rv.odVreme}</td>
                        <td>${rv.doVreme}</td>
                        <td>${rv.apoteka}</td>
                     <tr>`
    });

    $("#myTable").append(table);
}

function register() {

    console.log(usernm);

    var derm = {
        username : usernm,
        odDatum : $('#odDatum').val(),
        doDatum : $('#doDatum').val(),
        odVreme : $('#odVreme').val(),
        doVreme : $('#doVreme').val(),
        neradniDani : $('#neradniDani').val()
    }

    $.ajax({
        type:'POST',
        url: '/phadmin/hiredermatologist',
        contentType : 'application/json',
        beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.getItem('myToken'));
        },
        data : JSON.stringify(derm),
        success : function() {
            document.location.href = "dermatologistslist.html"
        }, error : function() {
            alert("ERROR");
        }
    })
}

