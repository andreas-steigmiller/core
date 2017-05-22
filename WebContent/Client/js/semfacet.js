$(document).foundation();

getBounds = function() {
    try {
        var map_bounds = new google.maps.LatLngBounds();
        return map_bounds;
    } catch (ReferenceError) {}
    return null;
}

//GLOBAL VARIABLES
var FOCUS = false;
var FOCUS_ID = "";
var SERVER_URL = "rest/WebService/"; //"http://semantic-facets.cs.ox.ac.uk:8080/semFacet/rest/WebService/";
var NEXT_PAGE = 0;
var END_RESULTS_REACHED = false;
var MAP = null;
var BOUNDS = getBounds();
var CHECKBOX_STACK = new Array();

$(window).scroll(function() {
    if ($(window).scrollTop() == $(document).height() - $(window).height()) {
        if (!END_RESULTS_REACHED && !FOCUS) {
            NEXT_PAGE++;
            var selected_facet_values_json = makeJsonFromSelectedValues(getSelectedFacetValueIds());
            $.getJSON(SERVER_URL + "getSnippets", {
                selected_facet_values: JSON.stringify(selected_facet_values_json),
                range_sliders: JSON.stringify(makeJsonFromRangeSliders()),
                datetime_sliders: JSON.stringify(makeJsonFromRangeDateTimeSliders()),
                search_keywords: $("#searchText").val(),
                active_page: NEXT_PAGE
            }, function(data) {
                try {
                    if (data.snippets == null || data.snippets.length == 0) {
                        END_RESULTS_REACHED = true;
                    } else {
                        generateSnippets(data);
                    }
                } catch (err) {
                    cleanPage();
                }
            });
        }
    }
});

// this method executes keyword search when enter key is pressed 
runSearch = function(e) {
    if (e.keyCode == 13) {
        getMainCategories();
    }
}

/**** BEGIN: METHODS TO GENERATE NAVIGATION MAP***/

// this method generates and updates the navigation map
updateNavigationMap = function() {
    $("#navigation_map").html("");
    var checked_objects = getSelectedFacetValueIds();
    if (checked_objects.length > 0) {
        var prev_level = Number.MAX_VALUE;
        var navigation_result = '<h5 style="color:darkslategrey;">Navigation Map:</h5>';
        for (var i = 0; i < checked_objects.length; i++) {
            var selected_value = $("#" + checked_objects[i]);
            var value_id = checked_objects[i];
            var value_name = selected_value.attr("object");
            var value_type = selected_value.attr("value_type");
            
            var value_label = selected_value.parent().text().replace(/\(([0-9]+\))/i,"");
            //old: var value_label = selected_value.parent().text();
            
            var predicate_name = selected_value.attr("predicate");
            var level = value_id.split("_").length;
            navigation_result += generateNavigationMapPredicate(value_id, predicate_name, checked_objects, (level - 3) * 10);
            navigation_result += generateNavigationMapCheckbox(value_id, value_name, value_type, value_label, predicate_name, (level - 2) * 10);
        }
        navigation_result += '</div>';
        $("#navigation_map").append(navigation_result);
    } else {
        removeFocus();
    }
}

//This method checks if we should add predicate for given value_id or it is already added for its sibling
generateNavigationMapPredicate = function(value_id, predicate, checked_objects, margin_size) {
    var lastIndex = value_id.lastIndexOf("_");
    var str = value_id.substring(0, lastIndex);
    var emptyPredicate = false;
    for (var j = 0; j < checked_objects.length; j++) {
        if (checked_objects[j] == value_id)
            break;
        if (checked_objects[j].match("^" + str) && checked_objects[j].split("_").length == value_id.split("_").length) {
            emptyPredicate = true;
            break;
        }
    }
    var focus_class = isSibling(value_id, FOCUS_ID) ? "onFocus" : "offFocus";
    return emptyPredicate ? "" : '<a style="margin-left:' + margin_size + 'px;font-size:12px;" class="' + focus_class + '" onclick="changeFocus(this)" >' + predicate + '</a>';
}

//This method gernerates chexbox for navigation map
generateNavigationMapCheckbox = function(value_id, value_name, value_type, value_label, predicate_name, margin_size) {
    return  '<label for="map_' + value_id + '" style="size:10px;margin-left:' + margin_size + 'px;font-size:12px;margin-bottom:0px;">' + 
                '<input id="map_' + value_id + '" type="checkbox" style="margin-right:10px; margin-bottom:0px;" object="' + value_name + '" predicate="' + predicate_name + '" value_type="' + value_type + '" class="map_checkbox" checked disabled="true">' + value_label + 
            '</label>';
}

/**** END: METHODS TO GENERATE NAVIGATION MAP***/


// this method populates json object from selected values
makeJsonFromSelectedValues = function(values) {
    var selectedValues = [];
    for (var i = 0; i < values.length; i++) {
    	var element = $("#" + values[i]);
        selectedValues.push({
            "id": values[i],
            "predicate": element.attr("predicate"),
            "object": element.attr("object"),
            "type": element.attr("value_type"),
            "parent": element.attr("parent"),
            "ranking": element.attr("ranking"),
            "parent_id": getParentId(values[i])
        });
    }
    return selectedValues;
}

getParentId = function(id) {
    var parent_id = "";
    var temp_id = id.split('_');
    if (temp_id.length > 3) {
        for (var i = 0; i < temp_id.length - 2; i++) {
            parent_id += temp_id[i] + "_";
        }
        parent_id = parent_id.substring(0, parent_id.length - 1);
    }
    return parent_id;
}

getSelectedFacetCheckboxIds = function(isSelected, toggledCheckbox) {
    var ids = [];
    $('input:checked').each(function() {
        var selected_value = this.id;
        var temp_id = "checkbox_" + toggledCheckbox;
        //This if makes sure that hanging children are not taken into acount if checkbox is unselecetd 
        if (isSelected || selected_value.substring(0, temp_id.length) != 0) {
            if ($(this).hasClass("facet_checkbox")) {
                ids.push(selected_value);
            }
        }
    });
    return ids;
}

getSameLevelFacetValueIds = function(selected_value) {
    var ids = [];
    var selected_id = $(selected_value).attr('id');
    var regex_siblings = "";
    var temp = selected_id.split("_");
    var regex_siblings = "";
    for (var i = 0; i < temp.length - 2; i++) {
        regex_siblings += temp[i] + "_";
    }
    regex_siblings = '^' + regex_siblings + '\\d+_\\d+$';
    var regex = new RegExp(regex_siblings);
    $('input').each(function() {
        if ($(this).attr('id').match(regex) && $(this).hasClass('facet_checkbox')) {
            ids.push(this.id);
        }
    });
    return ids;
}

getSelectedFacetValueIds = function() {
    var ids = [];
    $('input:checked').each(function() {
        if ($(this).hasClass("facet_checkbox") && $(this).is(":visible")) {
            ids.push(this.id);
        }
    });
    return ids;
}

isSibling = function(id1, id2) {
    var lastIndex1 = id1.lastIndexOf("_");
    var temp_id1 = id1.substring(0, lastIndex1);
    var lastIndex2 = id2.lastIndexOf("_");
    var temp_id2 = id2.substring(0, lastIndex2);
    return temp_id1 == temp_id2 ? true : false;
}

/*** BEGIN: METHODS TO CLEAN DIFFERENT PARTS OF UI ***/
cleanPage = function() {
    FOCUS = false;
    FOCUS_ID = "";
    $("#main-categories").html("");
    $("#navigation_map").html("");
    $("#facets").html("");
    CHECKBOX_STACK = new Array();
    cleanResults();
}

cleanFacets = function() {
    $("#facets").html("");
    $('#preloader').hide();
}

cleanResults = function() {
    $("#results").html("");
    $('#preloader').hide();
    MAP = null;
    BOUNDS = getBounds();
    $("#map-canvas").css("display", "none");
    resetPagination();
}

resetPagination = function() {
    NEXT_PAGE = 0;
    END_RESULTS_REACHED = false;
}

/*** END: METHODS TO CLEAN DIFFERENT PARTS OF UI ***/

/*** BEGIN: METHODS FOR EMPTY RESULTS ***/
getEmptyResultMessage = function() {
    return '<h5>There are no results, please refine the query.</h5>';
}

getEmptyFacetsMessage = function() {
    return '<h5>Unfortunatelly, there are no more options.</h5>';
}
    
/*** END: METHODS FOR EMPTY RESULTS ***/

/*** BEGIN: METHODS TO GET INITIAL CATEGORIES ***/
getMainCategories = function() {
    removeFocus();
    cleanPage();
    $('#preloader').show();
    $.getJSON(SERVER_URL + "getInitialFacet", {
        search_keywords: $("#searchText").val()
    }, function(data) {
        try {
            $('#preloader').hide();
            if (data.snippets == null || data.snippets.length == 0) {
                $("#results").html(getEmptyResultMessage());
            } else {
                generateSnippets(data);
                generateFirstFacet(data);
            }
        } catch (err) {
            cleanPage();
        }
    });
}


generateFirstFacet = function(data) {
    var facetValues = data.facetValues;
    var facet_name = data.firstFacetName.name;
    var facet_label = data.firstFacetName.label;
    var facet_type = data.firstFacetName.type;
    if (facet_label == null || facet_label == "")
        facet_label = facet_name;
    var active_facet = getInitialFacetHeader("0", facet_label, facet_type);
    $('#main-categories').html(active_facet);
    var facet_values_element = $("#facet_0");
    populateFacetValues(facetValues, facet_values_element, facet_name, "0");
}

updateSliderSpan = function(el) {
	element = $(el);
	element.siblings("div").first().html(element.val());
	executeUnselectedFacetValueQuery();
}
/*
generateSliderDiv = function(min, max, facet_name, facet_id) {
	var step = (max - min) / 100;
	var mid = min + (max - min) / 2;
	return '<div class="slider_div">' +
  				'<input type="range" min="' + min + '" max="' + max + '" step="' + step +'" onchange="updateSliderSpan(this);" facet_name="' + facet_name + '" facet_id="' + facet_id + '" style="width:100%">' +
    			'<div style="text-align:center;">' + mid + '</div>' +
			'</div>'
}
*/
generateSliderDiv = function(el){
	element = $(el);
	var facet_values_element = element.parent().siblings(".facet_values").first();
	var facet_name = element.attr("facet_name");
	var facet_id = element.attr("facet_id");
	var facet_type = element.attr("facet_type");
	var n_min = element.attr("min");
	var n_max = element.attr("max");
	var numberofnums = element.attr("numberOfNumerics")
	if (facet_type == '1') {
			facet_values_element.html('<div class = "range" id = "' + facet_id + '" facet_name ="' + facet_name + '" facet_id ="' + facet_id + '">' +
				'<div>' +
				' <div style ="text-align:center; margin-left: 5px; margin-bottom: 10px" ><span class = "range1">' + parseInt(n_min)  + '</span> - <span class = "range2">' + parseInt(n_max)  +  '</span></div>'+
				'<div class = "range_numeric" style = "margin-left: 20px; margin-right: 20px; margin-bottom: 15px"> </div>' + '</div>' +
				'</div>');
	} else if (facet_type == '2' || facet_type == '3') {
			facet_values_element.html('<div class = "range" id = "' + facet_id + '" facet_name ="' + facet_name + '" facet_id ="' + facet_id + '">' +
				' <div style ="text-align:center; margin-left: 5px; margin-bottom: 10px" ><span class = "range1">' + parseFloat(n_min)  + '</span> - <span class = "range2">' + parseFloat(n_max)  +  '</span></div>'+
				'<div class = "range_numeric" style = "margin-left: 20px; margin-right: 20px; margin-bottom: 15px"> </div>' + '</div>' );
	}
	populateSliderFacet(element,n_min, n_max, numberofnums);
}

populateSliderFacet = function(element, n_min, n_max, numberofnums) {
	var facet_id = element.attr("facet_id");
	var facet_type = element.attr("facet_type");
	var rangeSlider = document.getElementById(facet_id).getElementsByClassName('range_numeric')[0]; 
	if (facet_type == '1') {
		var numberOfInt = Math.round((parseInt(n_max) - parseInt(n_max))/parseInt(numberofnums));
		noUiSlider.create(rangeSlider, {
			start: [parseInt(n_min), parseInt(n_max)],
			connect: true,
			step: numberOfInt, 
			range: {
				'min': [parseInt(n_min)] ,
				'max': [parseInt(n_max)] 
				} 
		});
	} else if (facet_type == '2' || facet_type == '3') {
		var numberOfFloat = (parseFloat(n_max) - parseFloat(n_max))/parseFloat(numberofnums);
		noUiSlider.create(rangeSlider, {
			start: [parseFloat(n_min), parseFloat(n_max)],
			connect: true,
			step: numberOfFloat, 
			range: {
				'min': [parseFloat(n_min)] ,
				'max': [parseFloat(n_max)] 
				} 
		});
	}
	
	rangeSlider.noUiSlider.on('update',function(values){
							if (facet_type == '1') {
								$('#' + facet_id).find('.range1').html(Math.round(Number(values[0])));
								$('#' + facet_id).find('.range2').html(Math.round(Number(values[1])));
							}	else {
								$('#' + facet_id).find('.range1').html(Number(values[0]));
								$('#' + facet_id).find('.range2').html(Number(values[1]));
							}
								});
	rangeSlider.noUiSlider.on('change', function(){executeUnselectedFacetValueQuery();});
}


generateDateTimeSliderDiv = function(el){
	element = $(el);
	var facet_values_element = element.parent().siblings(".facet_values").first();
	var facet_name =  element.attr("facet_name");
	var facet_id = element.attr("facet_id");
	var d_min = element.attr("minYear");
	var d_max = element.attr("maxYear");
	facet_values_element.html('<div class ="time-range" id = "' + facet_id + '" facet_name ="' + facet_name + '" facet_id = "' + facet_id + '">' + 
				'<div> <div style="text-align:center;">Year</div> ' + 
				' <div style ="text-align:center; margin-left: 5px; margin-bottom: 5px" ><span class = "yearrange1">' + parseInt(d_min)  + '</span> - <span class = "yearrange2">' + parseInt(d_max)  +  '</span></div>'+
				'<div class = "range-years"> </div>' + '</div>' +
				'<div> <div style="text-align:center;">Month</div> ' +
				' <div style ="text-align:center; margin-left: 5px; margin-bottom: 5px" ><span class = "monthrange1">' + 01  + '</span> - <span class = "monthrange2">' + 12  +  '</span></div>' +
				'<div class = "range-months"></div>' + '</div>' +
				'<div> <div style="text-align:center;">Day</div> ' +
				' <div style ="text-align:center; margin-left: 5px; margin-bottom: 5px" ><span class = "dayrange1">' + 01  + '</span> - <span class = "dayrange2">' + 31  +  '</span></div>'  +
				'<div class = "range-days"></div>' + '</div>' +
				'<div> <div style="text-align:center;">Hour</div> ' +
				' <div style ="text-align:center; margin-left: 5px; margin-bottom: 5px" ><span class = "hourrange1">' + 00  + '</span> - <span class = "hourrange2">' + 23  +  '</span></div>' +
				'<div class = "range-hours"></div>' + '</div>' +
			'</div>');
	populateDateTimeFacet(element,d_min, d_max);
}

populateDateTimeFacet = function(element,d_min, d_max){
	var facet_id = element.attr("facet_id");
	var yearSlider = document.getElementById(facet_id).getElementsByClassName('range-years')[0];
	noUiSlider.create(yearSlider, {
	start: [parseInt(d_min),parseInt(d_max)],
	connect: true,
	step: 1,
	range: {
		'min': [parseInt(d_min)] ,
		'max': [parseInt(d_max)] 
	}
});
	
	
	var monthSlider = document.getElementById(facet_id).getElementsByClassName('range-months')[0];
	noUiSlider.create(monthSlider, {
		start: [1 , 12],
		connect: true,
		step: 1,
		range: {
			'min': [1],
			'max': [12]
		}
	});
	

	var daySlider = document.getElementById(facet_id).getElementsByClassName('range-days')[0];
	noUiSlider.create(daySlider, {
		start: [1 , 31],
		connect: true,
		step: 1,
		range: {
			'min': [1],
			'max': [31]
		}
	});
	

	var hourSlider = document.getElementById(facet_id).getElementsByClassName('range-hours')[0];
	noUiSlider.create(hourSlider, {
		start: [0 , 24],
		connect: true,
		step: 1,
		range: {
			'min': [0],
			'max': [23]
		}
	});
	yearSlider.noUiSlider.on('update',function(values){
							$('#' + facet_id).find('.yearrange1').html(Number(values[0]));
							$('#' + facet_id).find('.yearrange2').html(Number(values[1]));});
	yearSlider.noUiSlider.on('change', function(){executeUnselectedFacetValueQuery();});
	monthSlider.noUiSlider.on('update', function(values){ 
							$('#' + facet_id).find('.monthrange1').html(Number(values[0]));
							$('#' + facet_id).find('.monthrange2').html(Number(values[1]));});
	monthSlider.noUiSlider.on('change', function(){executeUnselectedFacetValueQuery();});
	daySlider.noUiSlider.on('change',function(values){
							$('#' + facet_id).find('.dayrange1').html(Number(values[0]));
							$('#' + facet_id).find('.dayrange2').html(Number(values[1]));
							executeUnselectedFacetValueQuery(values)});
	hourSlider.noUiSlider.on('change',function(values){
							$('#' + facet_id).find('.hourrange1').html(Number(values[0]));
							$('#' + facet_id).find('.hourrange2').html(Number(values[1]));
							executeUnselectedFacetValueQuery()});
}


/*
populateDateTimeFacet = function(d_min, d_max){
	$(".range-years").slider({
		range: true,
		min: d_min,
		max: d_max,
		values: [d_min,d_max],
		slide: function(event, ui) {
			executeUnselectedFacetValueQuery();
		}
	});
	$(el).find('.range-months', newContent).slider({
		range: true,
		min: 1,
		max: 12,
		step: 1,
		values: [1,12],
		slide: function(event, ui) {
			executeUnselectedFacetValueQuery();
		}
	});
	$(el).find('.range-days', newContent).slider({
		range: true,
		min: 1,
		max: 31,
		step: 1,
		values: [1,31],
		slide: function(event, ui) {
			executeUnselectedFacetValueQuery();
		}
	});
	$(el).find('.range-hours', newContent).slider({
		range: true,
		min: 0,
		max: 24,
		step: 1,
		values: [0,24],
		slide: function(event, ui) {
			executeUnselectedFacetValueQuery();
		}
	});
}
*/

makeJsonFromRangeSliders = function() {
	var rangeSliders = [];
	$(".range").each(function(){
		if ($(this).is(":visible")) {
			var numeric = document.getElementById($(this).attr("facet_id")).getElementsByClassName('range_numeric')[0];
			var min_value = Number(numeric.noUiSlider.get()[0]);
			var max_value = Number(numeric.noUiSlider.get()[1]);
			rangeSliders.push({
				"facet_id": $(this).attr("facet_id"),
				"facet_name": $(this).attr("facet_name"),
				"min": min_value,
				"max": max_value				
			});	
	}
	});
	return rangeSliders;
}

/*
makeJsonFromRangeSliders = function() {
	var rangeSliders = [];
	$(".slider_div").each(function() {
		if ($(this).is(":visible")) {
			var slider = $(this).children("input").first();
			rangeSliders.push({
				"facet_id": slider.attr("facet_id"),
				"facet_name": slider.attr("facet_name"),
				"value": slider.val()
			});
		}
	});
	return rangeSliders;
}
*/

makeJsonFromRangeDateTimeSliders = function() {
	var rangedatetimeSliders = [];
	$(".time-range").each(function(){
		if ($(this).is(":visible")) {
			var year = document.getElementById($(this).attr("facet_id")).getElementsByClassName('range-years')[0];
			var month = document.getElementById($(this).attr("facet_id")).getElementsByClassName('range-months')[0];
			var day = document.getElementById($(this).attr("facet_id")).getElementsByClassName('range-days')[0];
			var hour = document.getElementById($(this).attr("facet_id")).getElementsByClassName('range-hours')[0];
			var min_value = Number(year.noUiSlider.get()[0]) + "-" + ("0" + Number(month.noUiSlider.get()[0])).slice(-2) + "-" +  ("0" + Number(day.noUiSlider.get()[0])).slice(-2) + "T" + ("0" + Number(hour.noUiSlider.get()[0])).slice(-2) + ":" + "00:00";
			var max_value = Number(year.noUiSlider.get()[1]) + "-" + ("0" + Number(month.noUiSlider.get()[1])).slice(-2) + "-" +  ("0" + Number(day.noUiSlider.get()[1])).slice(-2) + "T" + ("0" + Number(hour.noUiSlider.get()[1])).slice(-2) + ":" + "59:59";
			rangedatetimeSliders.push({
				"facet_id": $(this).attr("facet_id"),
				"facet_name": $(this).attr("facet_name"),
				"min_value": min_value,
				"max_value": max_value				
			});
		
	}
	});
	return rangedatetimeSliders;
}

getInitialFacetHeader = function(id, facet_label, type) {
    return  '<h5 style="color:darkslategrey;">Filter by:</h5>' + 
            '<div class="facet_div">' + 
                '<div class="facet_header" style="background-color: #1B7488;">' + 
                    '<a style="color: white;font-size:12px;margin-top:7px; margin-left:3px; float:left;" onclick="toggleFacetNames(this);" facet_id="' + id + '" facet_type="' + type + '"> &#x25BC;</a>' + 
                    '<a onclick="searchFacetValues(this);">' + 
                        '<img src="Client/img/search.png" style="height:15px; margin-left:5px; margin-top:5px; float:left;" />' + 
                    '</a>' + 
                    '<h5 style="text-align:center; color: white;">'  + facet_label + '</h5>' + 
                '</div>' + 
                '<input type="text" class="hide" onkeyup="filterText(this);" />' + 
                '<div class="facet_values" id="facet_0" style="max-height: 200px; overflow-y: scroll;"></div>' + 
            '</div>' +
            '<div style="text-align:center; margin-top: 10px;" >' + 
                '<a id="more_options" style="font-size:15px;display:none;" onclick="getFacetNames()"> More options</a>' + 
            '</div>';
}

getFacetHeader = function(id, facet_name, facet_label, facet_type, min, max, minYear, maxYear, numberofnums) {
    return  '<div class="facet_div" style ="margin-top:0px;margin-bottom:2px;">' +
                '<div class="facet_header" style="background-color: #1B7488;">' +
                    '<a style="color: white;font-size:12px;margin-top:7px; margin-left:3px; float:left;" onclick="toggleFacetNames(this);" class="unexplored moreLess" facet_id="' + id + '" facet_name="' + facet_name + '" facet_type="' + facet_type + '" numberOfNumerics="' + numberofnums +'" min="' + min + '" max="' + max +'" minYear="' + minYear + '" maxYear="' + maxYear + '"> &#x25B6; </a>' + 
                    '<a onclick="refreshFacetValues(this);">' +
                        '<img src="Client/img/refresh.png" style="height:15px; margin-left:5px; margin-top:5px; float:left;" />' + 
                    '</a>' + 
                    '<a onclick="searchFacetValues(this);">' + 
                        '<img src="Client/img/search.png" style="height:15px; margin-left:5px; margin-top:5px; float:left;" />' + 
                    '</a>' + 
                    '<h5 style="text-align:center; margin-top: 10px;margin-bottom:0px;">' + 
                        '<a style="color: white;">' + facet_label + '</a>' + 
                    '</h5>' + 
                '</div>' +
                '<input type="text" class="hide" onkeyup="filterText(this);" />' + 
                '<div class="facet_values" style="max-height: 200px; overflow-y: scroll;margin-top:5px;"></div>' +
            '</div>';
}

searchFacetValues = function(el) {
	element = $(el).parent().next();
	if (element.hasClass("hide")) {
		element.removeClass("hide");
	} else {
		element.addClass("hide");
	}
}

filterText = function(el) {
    var element = $(el);
    var searchText = element.val().toLowerCase();

    $(element).siblings(".facet_values").find("li").each(function() {
        if (searchText.length > 0 && !$(this).children("label").first().children("input").first().prop("checked"))
            $(this).hide();
        else
            $(this).show();
    });

    $(element).siblings(".facet_values").find("input").each(function() {
        var text = $(this).parent().text().toLowerCase();
        if (text.indexOf(searchText) >= 0 && !$(this).parent().parent().hasClass("hideValue")) {
            $(this).parents().each(function() {
                if($(this).hasClass("facet_values"))
                    return false;
                if($(this).is("li"))
                    $(this).show();
            });
        }
    });
}

populateFacetValues = function(facetValues, facet_values_element, facet_name, facet_id) {
    var parent_name = '';
    var last_value_id = '';
    var hierarchyMap = {};
    for (var i = 0; i < facetValues.length; i++) {
        var value_name = facetValues[i].object;
        var value_type = facetValues[i].type;
        var value_label = facetValues[i].label;
        var value_parent = facetValues[i].parent;
        if (value_label == null || value_label == "")
            value_label = value_name;
        if (value_type == "1" || facet_id == "0")
            value_label = value_label.toUpperCase();
        
        var value_ranking = facetValues[i].ranking;

        hierarchyMap[value_name] = i;
        if (parent_name != value_parent) {
            parent_name = value_parent;
           
                if (parent_name == undefined ) {
                    facet_values_element.append("<ol><li>" + generateFacetValueCheckbox('checkbox_' + facet_id + '_' + i, value_name, value_label, value_type, value_parent, facet_name, value_ranking) + "</li></ol>");
                } else {
                    var element = $("#checkbox_" + facet_id + "_" + hierarchyMap[parent_name]).parent();
                    if (!element.prev().hasClass("collapsed_triangle"))
                        $('<span onclick="toggle(this)" class="collapsed_triangle"></span>').insertBefore(element);
                    $("<ol><li>" + generateFacetValueCheckbox('checkbox_' + facet_id + '_' + i, value_name, value_label, value_type, value_parent, facet_name, value_ranking) + "</li></ol>").insertAfter(element);
                    element.next().hide();
                }
            


        } else {
            var element = $("#" + last_value_id).parent().parent();
            $("<li>" + generateFacetValueCheckbox('checkbox_' + facet_id + '_' + i, value_name, value_label, value_type, value_parent, facet_name, value_ranking) + "</li>").insertAfter(element);
        }
        last_value_id = 'checkbox_' + facet_id + '_' + i;
    }
}

uncheckChildren = function(id) {
    $("#" + id).parent().next().find("input").prop("checked", false);
    //TODO reevaluate the query
}

checkParents = function(id) {
    var skip_first_li = true;
    var tempStack = new Array();
    $("#" + id).parents().each(function() {
        if ($(this).prop("tagName") == "LI") {
            if (!skip_first_li) {
                var parent = $(this).children("label").first().children("input").first();
                if (!parent.prop("checked")) {
                    parent.prop("checked", true);
                    tempStack.push(parent.attr("id"));
                }
            }
            skip_first_li = false;
        }
    });
    while (tempStack.length > 0) {
        CHECKBOX_STACK.push(tempStack.pop());
    }
}

toggle = function(el) {
    var element = $(el);
    if (element.hasClass('collapsed_triangle')) {
        element.removeClass('collapsed_triangle');
        element.addClass('expanded_triangle');
        element.siblings("ol").show();
    } else if (element.hasClass('expanded_triangle')) {
        var id = element.siblings("label").first().children("input").first().attr("id");
        uncheckChildren(id);
        element.addClass('collapsed_triangle');
        element.removeClass('expanded_triangle');
        element.siblings("ol").hide();
    }
}

executeGetFacetValuesQuery = function(element) {
    $('#preloader').show();
    var selected_facet_values_json = makeJsonFromSelectedValues(getSelectedFacetValueIds());
    $.getJSON(SERVER_URL + "getFacetValues", {
        selected_facet_values: JSON.stringify(selected_facet_values_json),
        range_sliders: JSON.stringify(makeJsonFromRangeSliders()),
        datetime_sliders: JSON.stringify(makeJsonFromRangeDateTimeSliders()),
        search_keywords: $("#searchText").val(),
        toggled_facet_name: $(element).attr("facet_name"),
        parent_checkbox_id: getParentCheckboxId($(element).attr("facet_id"))
    }, function(facetValues) {
        try {
            $('#preloader').hide();
            var facet_id = $(element).attr("facet_id");
            var facet_values_element = $(element).parent().siblings(".facet_values").first();
            var facet_name = $(element).attr("facet_name");
            populateFacetValues(facetValues, facet_values_element, facet_name, facet_id);
        } catch (err) {
            cleanPage();
        }
    });
}

/*** END: METHODS TO GET INITIAL CATEGORIES ***/

getSameLevelFacetNamesJson = function(selected_value) {
    var facetNames = [];
    var selected_id = $(selected_value).attr('id');
    var level = selected_id.split("_").length - 2;
    $('.moreLess').each(function() {
        if ($(this).attr("facet_id") != undefined) {
            if (level == $(this).attr("facet_id").split("_").length && $(selected_value).attr("predicate") != $(this).attr("facet_name")) {
                facetNames.push({
                    "id": $(this).attr("facet_id"),
                    "name": $(this).attr("facet_name")
                });
            }
        }
    });
    return facetNames;
}

adjustFacetDivHeight = function() {
    var cat_height = $('#main-categories').height();
    var result_height = $('#results').height();
    var facets_height = result_height - cat_height < 1000 ? "1000px" : (result_height - cat_height) + "px";
    $('#facets').css('max-height', facets_height);
    $('#facets').css('margin-bottom', '100px');
}


getFacetNames = function() {
    adjustFacetDivHeight();
    cleanFacets();
    $('#preloader').show();
    var selected_facet_values_json = makeJsonFromSelectedValues(getSelectedFacetValueIds());
    if (selected_facet_values_json.length < 1) {
        //nothing is selected reload everything
        getMainCategories();
    } else {
        $.getJSON(SERVER_URL + "getFacetNames", {
            selected_facet_values: JSON.stringify(selected_facet_values_json),
            range_sliders: JSON.stringify(makeJsonFromRangeSliders()),
            datetime_sliders: JSON.stringify(makeJsonFromRangeDateTimeSliders()),
            search_keywords: $("#searchText").val()
        }, function(data) {
            try {
                $('#preloader').hide();
                if (data.facetNames == null || data.facetNames.length == 0) {
                    $('#facets').html(getEmptyFacetsMessage());
                } else {
                    generateFacetNames(data);
                }
            } catch (err) {
            	$("#facets").append('<div>' + err + '</div>')
                //cleanPage();
            }
        });
    }
}


generateFacetNames = function(data) {
    for (var i = 0; i < data.facetNames.length; i++) {
        var facet_label = data.facetNames[i].label;
        if (facet_label == null || facet_label == "")
            facet_label = data.facetNames[i].name;
        if (data.facetNames[i].hasOwnProperty('minDateTime') && data.facetNames[i].hasOwnProperty('maxDateTime')){
        	//var content = $('#facets').find("a[facet_id='" + (i+1) + "']").first().data('minDateTime');
        	var minYear = data.facetNames[i].minDateTime.date.year;
        	var maxYear = data.facetNames[i].maxDateTime.date.year;
        }
        var facet_data = getFacetHeader(i + 1, data.facetNames[i].name, facet_label, data.facetNames[i].type, data.facetNames[i].min, data.facetNames[i].max, minYear, maxYear, data.facetNames[i].numberOfNumerics);
        $("#facets").append(facet_data);

    }
}


//new method with ranking
generateFacetValueCheckbox = function(value_id, value_name, value_label, value_type, value_parent, predicate_name, value_ranking) {
    return  '<label for="' + value_id + '" >' + 
                '<input id="' + value_id + '" type="checkbox" style="margin-right:10px;margin-bottom:0px;" parent="' + value_parent + '" object="' + value_name + '" predicate="' + predicate_name + '" value_type="' + value_type + '" ranking="' + value_ranking + '" onclick="adjustFacets(this)" class="facet_checkbox">' + cleanFacetValues(value_label) +' ('+ value_ranking +') '+ 
            '</label>';
}

// old method
/*
generateFacetValueCheckbox = function(value_id, value_name, value_label, value_type, value_parent, predicate_name) {
    return  '<label for="' + value_id + '" >' + 
                '<input id="' + value_id + '" type="checkbox" style="margin-right:10px;margin-bottom:0px;" parent="' + value_parent + '" object="' + value_name + '" predicate="' + predicate_name + '" value_type="' + value_type + '" onclick="adjustFacets(this)" class="facet_checkbox">' + cleanFacetValues(value_label) + 
            '</label>';
}
*/


showHideMoreOptions = function() {
    var selected_values = getSelectedFacetValueIds();
    if (selected_values.length > 0)
        $("#more_options").css("display", "inline");
    else
        $("#more_options").css("display", "none");
}

adjustFacets = function(selected_value) {
    showHideMoreOptions();
    if ($(selected_value).attr("checked")) {
        pushCheckboxStack($(selected_value).attr("id"));
        updateNavigationMap();
        if (FOCUS) {
            executeFocusQuery();
        } else {
            executeHideUnhideFacetValues(selected_value);
            executeSelectedFacetValueQuery(selected_value);
        }
    } else {
        popCheckboxStack($(selected_value).attr("id"));
        if ($(selected_value).parent().next().hasClass("facet_div")) {
            cleanExpandedFacets(selected_value);
        }
        updateNavigationMap();
        if (FOCUS) {
            executeFocusQuery();
        } else {
            executeHideUnhideFacetValues(selected_value);
            executeUnselectedFacetValueQuery();
        }
    }
}

popCheckboxStack = function(id) {
    var stack_id = "";
    while (stack_id != id && CHECKBOX_STACK.length > 0) {
        stack_id = CHECKBOX_STACK.pop();
         $("#" + stack_id).prop("checked", false);
    }
}

pushCheckboxStack = function(id) {
    checkParents(id);
    CHECKBOX_STACK.push(id);
}

executeHideUnhideFacetValues = function(selected_value) {
    cleanResults();
    $('#preloader').show();
    var selected_facet_values_json = makeJsonFromSelectedValues(getSelectedFacetValueIds());
    var toggled_value_json = makeJsonFromSelectedValues(selected_value);
    var same_level_sibling = getSameLevelFacetValueIds(selected_value);
    var same_level_sibling_json = makeJsonFromSelectedValues(same_level_sibling);
    var same_level_facet_names_json = getSameLevelFacetNamesJson(selected_value);
    $.ajax({
        url: SERVER_URL + "hideUnhideFacetValues",
        type: "POST",
        data: {
            selected_facet_values: JSON.stringify(selected_facet_values_json),
            range_sliders: JSON.stringify(makeJsonFromRangeSliders()),
            datetime_sliders: JSON.stringify(makeJsonFromRangeDateTimeSliders()),
            toggled_facet_value_id: $(selected_value).attr("id"),
            search_keywords: $("#searchText").val(),
            same_level_siblings: JSON.stringify(same_level_sibling_json),
            facet_names: JSON.stringify(same_level_facet_names_json)
        },
        contentType: "application/x-www-form-urlencoded",
        success: function(data) {
            try {
                for (var i = 0; i < data.facetValues.length; i++)
                	showHideValue(data.facetValues[i].isHidden, data.facetValues[i].id, data.facetValues[i].ranking);
                for (var i = 0; i < data.facetNames.length; i++)
                	showHideFacetName(data.facetNames[i].hidden, data.facetNames[i].id);
               $('#preloader').hide();
            } catch (err) {
                cleanPage();
            }
        }
    });
}

showHideFacetName = function(hidden, id) {
    $('.moreLess').each(function() {
        if ($(this).attr("facet_id") != undefined && $(this).attr("facet_id") == id) {
            if (hidden)
                $(this).parent().parent().hide();
            else
                $(this).parent().parent().show();
        }
    });
}


showHideValue = function(hidden, id, ranking) {
    var element = $("#" + id).parent().parent();
    var element2 = $("#" + id);
    if (hidden && !element.hasClass("hideValue") && !element.attr('checked')) {
        element.hide();
        element.addClass("hideValue");
    } else if (!hidden && element.hasClass("hideValue")){
        element.removeClass("hideValue");
        element.show();
        element2.attr("ranking", (element2.attr("ranking")).replace(/[0-9]+/i,ranking));
        element.html(element.html().replace(/\(([0-9]+\))/i,"("+ranking+")"));
    }
    else{
    	element2.attr("ranking", (element2.attr("ranking")).replace(/[0-9]+/i,ranking));
    	element.html(element.html().replace(/\(([0-9]+\))/i,"("+ranking+")"));
    	if(CHECKBOX_STACK.indexOf(id) != -1){
    		element.find("input").prop("checked", true);
    	}
    }
}

/*
showHideValue = function(hidden, id) {
    var element = $("#" + id).parent().parent();
    if (hidden && !element.hasClass("hideValue") && !element.attr('checked')) {
        element.hide();
        element.addClass("hideValue");
    } else if (element.hasClass("hideValue")){
        element.removeClass("hideValue");
        element.show();
    }
}
*/

cleanExpandedFacets = function(selected_value) {
    var next = $(selected_value).parent().next();
    while (next.hasClass("facet_div")) {
        next.remove();
        next = $(selected_value).parent().next();
    }
}

executeSelectedFacetValueQuery = function(selected_value) {
    cleanResults();
    $('#preloader').show();
    var selected_facet_values_json = makeJsonFromSelectedValues(getSelectedFacetValueIds());
    var toggled_value_json = makeJsonFromSelectedValues(selected_value);
    if (selected_facet_values_json.length < 1) {
        getMainCategories();
    } else {
        $.getJSON(SERVER_URL + "getSelectedFacetValueData", {
            selected_facet_values: JSON.stringify(selected_facet_values_json),
            range_sliders: JSON.stringify(makeJsonFromRangeSliders()),
            datetime_sliders: JSON.stringify(makeJsonFromRangeDateTimeSliders()),
            toggled_facet_value_id: $(selected_value).attr("id"),
            search_keywords: $("#searchText").val()
        }, function(data) {
            try {
                $('#preloader').hide();
                if (data.snippets == null || data.snippets.length == 0) {
                    $("#results").html(getEmptyResultMessage());
                } else {
                    generateSnippets(data);
                    generateNestedFacetNames(data, selected_value);
                    
                    //TODO: we have also to update the counters in the facet values of the response
                    // we have to use the Stack CHECKBOX_STACK
                }
            } catch (err) {
                cleanPage();
            }
        });
    }
}


generateNestedFacetNames = function(data, selected_value) {
    var parent_value_id = ($(selected_value).attr("id")).replace("checkbox_", "");
    for (var i = 0; i < data.facetNames.length; i++) {
        var facet_label = data.facetNames[i].label;
        if (facet_label == null || facet_label == "")
            facet_label = data.facetNames[i].name;
        var facet_data = getNestedFacetHeader(parent_value_id + '_' + i, data.facetNames[i].name, facet_label, data.facetNames[i].type, data.facetNames[i].min, data.facetNames[i].max);
        $(facet_data).insertAfter($(selected_value).parent());
    }
}

getNestedFacetHeader = function(id, facet_name, facet_label, facet_type, min, max, minYear, maxYear) {
    return  '<div class="facet_div" style ="margin-left:20px;">' + 
                '<div class="facet_header">' + 
                    '<a style="font-size:12px; margin-right:10px; margin-top:7px; float:left; display: block" onclick="toggleFacetNames(this);" class="unexplored moreLess" facet_id="' + id + '" facet_name="' + facet_name+ '" facet_type="' + facet_type + '" min="' + min +'" max="' + max + '" minYear="' + minYear +'" maxYear="' + maxYear + '"> &#x25B6; </a>' + 
                    '<a onclick="refreshFacetValues(this);">' + 
                        '<img src="Client/img/refresh_black.png" style="height:15px; margin-top:5px; float:left;" />' + 
                    '</a>' + 
                    '<a onclick="searchFacetValues(this);">' + 
                        '<img src="Client/img/search_black.png" style="height:15px; margin-left:5px; margin-top:5px; float:left;" />' + 
                    '</a>' + 
                    '<h5 style="margin-bottom:0px;">' + 
                        '<a style="color:darkslategrey;">' + facet_label + '</a>' + 
                    '</h5>' + 
                '</div>' + 
                '<input type="text" class="hide" onkeyup="filterText(this);" />' + 
                '<div class="facet_values" style="max-height: 200px; overflow-y: scroll;margin-top:5px;"></div>' +
            '</div>';
}

toggleFacetNames = function(el) {
	element = $(el);
    if (element.hasClass("unexplored")) {
        element.removeClass("unexplored");
        element.html("&#x25BC;");
        if (element.attr("facet_type") == 1 || element.attr("facet_type") == 2 || element.attr("facet_type") == 3) {
        	generateSliderDiv(el);
        	//element.parent().siblings(".facet_values").first().html(generateSliderDiv(element.attr("min"), element.attr("max"), element.attr("facet_name"), element.attr("facet_id")));
        } else if (element.attr("facet_type") == 4) { 
        	generateDateTimeSliderDiv(el);
        	//element.parent().siblings(".facet_values").first().html(generateDateTimeSliderDiv(element.attr("minDateTime"), element.attr("maxDateTime", element.attr("facet_name"), element.attr("facet_id")));
        } else {
        	executeGetFacetValuesQuery(el);
    	}
    } else if (element.hasClass("more")) {
        element.removeClass("more");
        element.html("&#x25BC;");
        if (element.attr("facet_type") == 1 || element.attr("facet_type") == 2 || element.attr("facet_type") == 3 ||  element.attr("facet_type") == 4) {
        	element.parent().siblings(".facet_values").first().show();
        } else {
	        element.parent().siblings(".facet_values").find("li").each(function() {
	            if (!$(this).hasClass("hideValue"))
	                $(this).show();
	        });
    	}
    } else {
        element.addClass("more");
        element.html("&#x25B6;");
        if (element.attr("facet_type") == 1 || element.attr("facet_type") == 2 || element.attr("facet_type") == 3 ||  element.attr("facet_type") == 4) {
        	element.parent().siblings(".facet_values").first().hide();
        } else {
	        element.parent().siblings(".facet_values").find("li").each(function() {
	            if (!$(this).children("label").first().children("input").first().prop("checked")) {
	                $(this).hide();
	            }
	        });
    	}
    }
}

refreshFacetValues = function(element) {
    var expandElement = $(element).prev();
    expandElement.parent().siblings(".facet_values").html("");
    expandElement.removeClass("unexplored");
    expandElement.html("&#x25BC;");
    executeGetFacetValuesQuery(expandElement);
    executeUnselectedFacetValueQuery();
    updateNavigationMap();
}

getParentCheckboxId = function(facetId) {
    var pos = facetId.lastIndexOf("_");
    if (pos == -1)
        return null;
    return "checkbox_" + facetId.substring(0, pos);
}

executeUnselectedFacetValueQuery = function() {
    cleanResults();
    $('#preloader').show();
    var selected_facet_values_json = makeJsonFromSelectedValues(getSelectedFacetValueIds());
    if (selected_facet_values_json.length < 1) {
        getMainCategories();
    } else {
        $.getJSON(SERVER_URL + "getUnselectedFacetValueData", {
            selected_facet_values: JSON.stringify(selected_facet_values_json),
            range_sliders: JSON.stringify(makeJsonFromRangeSliders()),
            datetime_sliders: JSON.stringify(makeJsonFromRangeDateTimeSliders()),
            search_keywords: $("#searchText").val()
        }, function(data) {
            try {
                $('#preloader').hide();
                if (data.snippets == null || data.snippets.length == 0) {
                    $("#results").html(getEmptyResultMessage());
                } else {
                    generateSnippets(data);
                }
            } catch (err) {
                cleanPage();
            }
        });
    }
}

/*** BEGIN: METHODS FOR FOCUS REFOCUS ***/
removeFocus = function() {
    FOCUS = false;
    FOCUS_ID = "";
    $(".onFocus").each(function() {
        $(this).removeClass("onFocus");
        $(this).addClass("offFocus");
    });
}

addFocus = function(element) {
    FOCUS = true;
    $(element).removeClass("offFocus");
    $(element).addClass("onFocus");
}

getSelectedValuesForFocus = function(element) {
    var ids = [];
    FOCUS_ID = $(element).next().children("input").first().attr("id").replace("map_", "");
    $(element).siblings("label").find("input:checked").each(function() {
        if (isSibling(FOCUS_ID, this.id.replace("map_", ""))) {
            ids.push(this.id.replace("map_", ""));
            FOCUS_ID = this.id.replace("map_", "");
        }
    });
    return ids;
}

changeFocus = function(element) {
    if ($(element).hasClass("offFocus")) {
        removeFocus();
        addFocus(element);
        executeFocusQuery();
    } else {
        removeFocus();
        executeUnselectedFacetValueQuery();
    }
}

executeFocusQuery = function() {
    cleanResults();
    $('#preloader').show();
    var seleced_focus_values = getSelectedValuesForFocus($('.onFocus').first());
    var seleced_focus_values_json = makeJsonFromSelectedValues(seleced_focus_values);
    var selected_facet_values_json = makeJsonFromSelectedValues(getSelectedFacetValueIds());
    $.getJSON(SERVER_URL + "getDataForFocus", {
        focus_values: JSON.stringify(seleced_focus_values_json),
        selected_facet_values: JSON.stringify(selected_facet_values_json),
        range_sliders: JSON.stringify(makeJsonFromRangeSliders()),
        datetime_sliders: JSON.stringify(makeJsonFromRangeDateTimeSliders()),
        search_keywords: $("#searchText").val()
    }, function(data) {
        try {
            $('#preloader').hide();
            if (data.snippets == null || data.snippets.length == 0) {
                $("#results").html(getEmptyResultMessage());
            } else {
                generateSnippets(data);
            }
        } catch (err) {
            cleanPage();
        }
    });
}

/*** END: METHODS FOR FOCUS REFOCUS ***/

/**** BEGIN: METHOD TO GENERATE SNIPPETS ***/
generateSnippets = function(data) {
    for (var i = 0; i < data.snippets.length; i++) {
        setMarkers(data.snippets[i]);

        var title = data.snippets[i].title;
        if (title == null || title == "") {
            title = cleanFacetValues(data.snippets[i].id);
        }

        var description = data.snippets[i].description;
        if (description == null || description == "") {
            description = "";
        } else {
        	description += "</br>";
        }

        var extra1 = data.snippets[i].extra1;
        if (extra1 == null || extra1 == "") {
            extra1 = "";
        } else {
        	extra1 += "</br>";
        }

        var extra2 = data.snippets[i].extra2;
        if (extra2 == null || extra2 == "") {
            extra2 = "";
        } else {
        	extra2 += "</br>";
        }

        var extra3 = data.snippets[i].extra3;
        if (extra3 == null || extra3 == "") {
            extra3 = "";
        } else {
        	extra3 += "</br>";
        }

        var url = data.snippets[i].url;
        url = url.replace(/\"/g, "");
        if (url == null || url == "")
            url = cleanFacetValues(data.snippets[i].id);
        var image = data.snippets[i].image;
        var display = "block";
        if (image == "")
            display = "none";
        var result_string = getSnippetDiv(url, title, display, image, description, extra1, extra2, extra3);
        $("#results").append(result_string);
    }

    if (MAP != null && BOUNDS != null) {
        if (BOUNDS.getNorthEast().equals(BOUNDS.getSouthWest())) {
            var extendPoint1 = new google.maps.LatLng(BOUNDS.getNorthEast().lat() + 0.1, BOUNDS.getNorthEast().lng() + 0.1);
            var extendPoint2 = new google.maps.LatLng(BOUNDS.getNorthEast().lat() - 0.1, BOUNDS.getNorthEast().lng() - 0.1);
            BOUNDS.extend(extendPoint1);
            BOUNDS.extend(extendPoint2);
        }
        MAP.fitBounds(BOUNDS);
    }

}

getSnippetDiv = function(url, title, display, image, description, extra1, extra2, extra3) {
    var keywords = $("#searchText").val();
    return  '<div style="height-min:250px; clear:both;">' +
                '<a style="font-size:14px" target="_blank" href="' + url + '">' + hiliter(keywords, title) + '</a>' + 
                '<div style="float:left; min-height: 150px; display: ' + display + '">' + 
                    '<img src="' + image + '" style="width:100px; margin:4px; max-height:180px"/>' + 
                '</div>' + 
                '<div>' + 
                    '<p style="font-size:14px">' + hiliter(keywords, description) + hiliter(keywords, extra1) + hiliter(keywords, extra2) + hiliter(keywords, extra3) + '</p>' + 
                '</div>' + 
            '</div>';
}

hiliter = function(keywords, text) {
        if (keywords.length < 1)
            return text;
        var splitKeywords = keywords.split(" ");
        for (var i = 0; i < splitKeywords.length; i++) {
            text = text.replace(new RegExp('(' + splitKeywords[i] + ')', 'gi'), '<b>$1</b>');
        }
        return text;
    }
    /**** END: METHOD TO GENERATE SNIPPETS ***/

// This function cleans ids from underscores and removes the last weird number if it exists. It is somewhat specific to dbpedia
cleanFacetValues = function(value_name) {
    //special cleaning for yago data
    if (value_name.indexOf("wordnet_") != -1 || value_name.indexOf("WORDNET_") != -1) {
        var clean_name = value_name.replace("wordnet_", "").replace("WORDNET_", "");
        temp_id = clean_name.split("_");
        if (!isNaN(temp_id[temp_id.length - 1])) {
            temp_id.splice(temp_id.length - 1, 1);
        }
        return temp_id.join(" ");
    } else {
        return value_name.split("_").join(" ");
    }
}

String.prototype.trunc = String.prototype.trunc || function(n) {
    return this.length > n ? this.substr(0, n - 1) + '&hellip;' : this;
};


// MAPS

setMarkers = function(snippet) {
    if (snippet.lat != null && snippet.lng != null && BOUNDS != null) {
        $("#map-canvas").css("display", "block");
        if (MAP == null)
            MAP = new google.maps.Map($('#map-canvas')[0], {});
        var latLng = new google.maps.LatLng(snippet.lat, snippet.lng);
        var marker = new google.maps.Marker({
            position: latLng,
            map: MAP,
            title: snippet.id
        });
        BOUNDS.extend(marker.position);
    }
}