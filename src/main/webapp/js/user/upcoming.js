function reloadUpcoming() {
    $('#upcoming_body').html("");
    $.ajax({
        type:'GET',
        url: '/api/user/consults',
        contentType : 'application/json',
        beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.getItem('myToken'));
        },
        success : function(data) {
            fillUpcoming(data)
            refreshToken()
        },
        error : function() {
            console.log('Could not load allergies')
            showError('Error message', 'Could not load upcoming appointments.')
        }
        
    })
}


function fillUpcoming(data) {

    var body = $('#upcoming_body')
    var html = "";
    data.forEach(function(v) {
        /*Type of visit</th>
        <th scope="col" onclick="sortTable(1,'upcoming_table')">Doctor</th>
        <th scope="col" onclick="sortTable(2,'upcoming_table')">Time</th>
        <th scope="col" onclick="sortTable(3,'upcoming_table')">Date</th>
        <th scope="col" onclick="sortTable(4,'upcoming_table')">Pharmacy</th> */

        var date = new Date(v.datum)
        
        //console.log(ed.getTimezoneOffset())
        //ed.setMinutes(ed.getMinutes() + ed.getTimezoneOffset())
        var btn_id = 'button-' + v.id  
        html += '<tr>'
        html += '<td>' + v.vrsta +'</td>'
        html += '<td>' + v.zaposleni.ime + '&nbsp'  + v.zaposleni.prezime + '</td>'
        html += '<td>' + v.vreme +'</td>'
        html += '<td>' + date.toLocaleDateString('fr-CA') +'</td>'
        html += '<td>' + v.apoteka.naziv +'</td>'
        html += '<td>' + '<button onclick="cancelConsult(this.id)" class="btn btn-info-allergies" id=' + '"' + btn_id + '">Cancel </button></td>'
        html += '</tr>'
    })

    body.html(html)
}

function cancelConsult(btn_id) {

    var obj = {
        "id" : btn_id.split("-")[1]
    }

    $.ajax({
        type:'POST',
        url: '/api/user/consults/cancel',
        contentType : 'application/json',
        beforeSend: function (xhr) {
            xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.getItem('myToken'));
        },
        data : JSON.stringify(obj),
        success : function(data) {
            console.log('deleted')
            reloadUpcoming()
        },
        error : function() {
            console.log('nope')
            showError('Error message', 'Could not cancel appointment.')
        }
        
    })


}