/*
 * Pull local Farers market data from the USDA API and display on * Google Maps
 * using GeoLocation or user input zip code. By Paul Dessert *
 * www.pauldessert.com | www.seedtip.com
 */

function setCookie(cname, cvalue, exdays) {
  var d = new Date();
  d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
  var expires = "expires=" + d.toUTCString();
  document.cookie = cname + "=" + cvalue + "; " + expires;
}

function getCookie(cname) {
  var name = cname + "=";
  var ca = document.cookie.split(';');
  for (var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == ' ')
      c = c.substring(1);
    if (c.indexOf(name) === 0) return c.substring(name.length, c.length);
  }
  return "";
}

$(function() {

  var marketId = []; // returned from the API
  var allLatlng = []; // returned from the API
  var allLinks = []; // returned from the API
  var allData = {
    "HK": [],
    "K": [],
    "ST": [],
    "TM": []
  };
  var infowindow = null;
  var pos;
  var userCords;
  var tempMarkerHolder = [];

  var filterState = {
    "HK": true,
    "K": true,
    "ST": true,
    "TM": true,
  };

  var loadFilterStateFromCookie = function() {
    var keys = ["HK", "K", "ST", "TM"];
    for ( var key in keys) {

      var tag = keys[key];
      var cookieValue = getCookie(tag);
      console.log(cookieValue);
      if (cookieValue == "false")
        filterState[tag] = false;
      else
        filterState[tag] = true;
      $(".areaCheckBox[name=" + tag + "]").prop("checked", filterState[tag]);
    }

  };
  loadFilterStateFromCookie();

  // map options
  var mapOptions = {
    zoom: 11,
    center: new google.maps.LatLng(22.352734, 114.1277),
    panControl: false,
    panControlOptions: {
      position: google.maps.ControlPosition.BOTTOM_LEFT
    },
    zoomControl: true,
    zoomControlOptions: {
      style: google.maps.ZoomControlStyle.LARGE,
      position: google.maps.ControlPosition.RIGHT_CENTER
    },
    scaleControl: false

  };

  // Adding infowindow option
  infowindow = new google.maps.InfoWindow({
    content: "holding..."
  });

  // Fire up Google maps and place inside the map-canvas div
  map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);

  var resolveColor = function(state) {
    if (state == "TRAFFIC GOOD") return "green";
    if (state == "TRAFFIC BAD") return "red";
    return "yellow";
  };

  var resolveRoadType = function(roadType) {
    if (roadType == "MAJOR ROUTE") return 5;
    return 3;
  };

  var deleteAllLines = function() {
    for (var i = 0; i < allLinks.length; i++) {
      allLinks[i].setMap(null);
    }
    allLinks = [];
  };

  var isInitialRequest = true;

  var onMouseEnter = function(e) {
    $("#infoSpeed").text("demo");
  };

  var onMouseLeave = function(e) {
    $("#infoSpeed").text("--");
  };

  var fnProcessData = function(data) {
    // alert('hi');
    var latestTime = "";

    deleteAllLines();

    for ( var key in data) {

      var startLat = data[key].linkInfo.startLat;
      var startLng = data[key].linkInfo.startLng;
      var startLatLng = new google.maps.LatLng(startLat, startLng);

      var endLat = data[key].linkInfo.endLat;
      var endLng = data[key].linkInfo.endLng;
      var endLatLng = new google.maps.LatLng(endLat, endLng);

      var color = resolveColor(data[key].roadSaturationLevel);
      var stroke = resolveRoadType(data[key].roadType);
      var startNode = data[key].linkInfo.startNode;
      var endNode = data[key].linkInfo.endNode;

      var lineSymbol = {
        path: google.maps.SymbolPath.FORWARD_OPEN_ARROW
      };

      var link = new google.maps.Polyline({
        path: [startLatLng, endLatLng],
        icons: [{
          icon: lineSymbol,
          offset: "100%"
        }],
        strokeColor: color,
        strokeWeight: stroke,
        map: map
      });
      // console.log("added to map");

      link.customdata = data[key];

      google.maps.event.addListener(link, 'mouseover', onMouseEnter);

      // assuming you want the InfoWindow to close on mouseout
      google.maps.event.addListener(link, 'mouseout', onMouseLeave);

      allLinks.push(link);
      allLatlng.push(startLatLng, endLatLng);

      latestTime = data[key].captureDate;
    }

    if (isInitialRequest) {

      var bounds = new google.maps.LatLngBounds();
      // Go through each...
      for (var i = 0, LtLgLen = allLatlng.length; i < LtLgLen; i++) {
        // And increase the bounds to take this point
        bounds.extend(allLatlng[i]);
      }
      // Fit these bounds to the map
      map.fitBounds(bounds);

      isInitialRequest = false;
      allLatlng = [];
    }
    $("#updatedTime").text(latestTime);
    // console.log("latest data time: " + latestTime);

  };

  var fnFilter = function() {
    // refresh
    var filteredData = [];

    for ( var key in filterState) {
      var checked = filterState[key];
      console.log("key: " + key + "checked: " + checked);
      if (checked) filteredData = filteredData.concat(allData[key]);
    }

    return filteredData;

  };

  var fnReload = function() {
    // grab form data
    $.ajax({
      type: "GET",
      contentType: "application/json; charset=utf-8",
      url: "/api/traffic/speedmap/latest",
      success: function(freshData) {

        allData = {
          "HK": [],
          "K": [],
          "ST": [],
          "TM": []
        };

        for ( var key in freshData) {
          allData[freshData[key].region].push(freshData[key]);
        }
        console.log(allData);

        var filteredData = fnFilter();

        fnProcessData(filteredData);
      }
    });
  };

  fnReload();
  setInterval(fnReload, 20000);

  $(".areaCheckBox").change(function(e) {
    var key = $(this).attr("name");
    var checked = $(this).prop("checked");

    filterState[key] = checked;
    setCookie(key, checked, 30);

    var filteredData = fnFilter();
    isInitialRequest = true;

    fnProcessData(filteredData);
  });

  // submitting
});
