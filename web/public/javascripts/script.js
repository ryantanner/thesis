var thesis = thesis || {};

function init() {
   var myOptions = {
     center: new google.maps.LatLng(39.50, -98.35),
     zoom: 3,
     mapTypeId: google.maps.MapTypeId.ROADMAP
    };

    thesis.map = new google.maps.Map(document.getElementById("map_canvas"),
                                  myOptions);

    loadMarkers(thesis.entityId, thesis.entityValue);

}

function loadMarkers(entityId, entityValue) {

    $.getJSON('/location-info/' + 1887, function(data) {
        $.each(data, function(i, m) {
            var myLatLng = new google.maps.LatLng(m.lat, m.lng);

            var infoWindow = new google.maps.InfoWindow({
                content: windowContent(m.entityId, m.entityValue, entityId, entityValue)
            });

            var marker = new google.maps.Marker({
                position: myLatLng,
                map: thesis.map,
                title: m.entityValue
            });

            google.maps.event.addListener(marker, 'click', function () {
                infoWindow.open(thesis.map, marker);
                if(thesis.lastWindow)
                    thesis.lastWindow.close();
                thesis.lastWindow = infoWindow;
            });
        });
    });

}

function windowContent(id, title, entityId, entity)   {
    return '<div id="content">' + 
            "<h2>" + title + "</h2>" +
            '<a href="/connections/' + id + '">View Connections</a>' + "<br />" +
            '<a href="/intersection/' + entityId + '/' + id + '">Connections to ' + 
            entity + '</a>';
}


$(document).ready(function($) {

    /*
    $('#map_canvas').gmap().bind('init', function () {
        $.getJSON('/location-info/' + 1887, function(data) {
            $.each(data, function(i, m) {
                $('#map_canvas').gmap('addMarker', { 
                    'position': new google.maps.LatLng(m.lat, m.lng),
                    'bounds': true
                });
            });
        });
    });
*/

    init();


});
