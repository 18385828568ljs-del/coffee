/**
 * 閫氱敤鏂规硶灏佽澶勭悊
 * Copyright (c) 2026 coffee-mall-admin 
 */

var startLayDate;
var endLayDate;
$(function() {
	
    //  layer鎵╁睍鐨偆
    if (window.layer !== undefined) {
        layer.config({
            extend: 'moon/style.css',
            skin: 'layer-ext-moon'
        });
    }
	
    // 鍥炲埌椤堕儴缁戝畾
    if ($.fn.toTop !== undefined) {
        $('#scroll-up').toTop();
    }
	
    // select2澶嶉€夋浜嬩欢缁戝畾
    if ($.fn.select2 !== undefined) {
        $.fn.select2.defaults.set( "theme", "bootstrap" );
        $("select.form-control:not(.noselect2)").each(function () {
            $(this).select2().on("change", function () {
                $(this).valid();
            })
        })
    }
	
    // iCheck鍗曢€夋鍙婂閫夋浜嬩欢缁戝畾
    if ($.fn.iCheck !== undefined) {
        $(".check-box:not(.noicheck),.radio-box:not(.noicheck)").each(function() {
            $(this).iCheck({
                checkboxClass: 'icheckbox-blue',
                radioClass: 'iradio-blue',
            })
        })
    }
	
    // 鍙栨秷鍥炶溅鑷姩鎻愪氦琛ㄥ崟
    $(document).on("keypress", ":input:not(textarea):not([type=submit])", function(event) {
        if (event.keyCode == 13) {
            event.preventDefault();
        }
    });
	 
    // laydate 鏃堕棿鎺т欢缁戝畾
    if ($(".select-time").length > 0) {
       layui.use('laydate', function() {
            var laydate = layui.laydate;
            startLayDate = laydate.render({
                elem: '#startTime',
                max: $('#endTime').val(),
                theme: 'molv',
                type: $('#startTime').attr("data-type") || 'date',
                trigger: 'click',
                done: function(value, date) {
                    // 缁撴潫鏃堕棿澶т簬寮€濮嬫椂闂?
                    if (value !== '') {
                        endLayDate.config.min.year = date.year;
                        endLayDate.config.min.month = date.month - 1;
                        endLayDate.config.min.date = date.date;
                    } else {
                        endLayDate.config.min.year = '';
                        endLayDate.config.min.month = '';
                        endLayDate.config.min.date = '';
                    }
                }
            });
            endLayDate = laydate.render({
                elem: '#endTime',
                min: $('#startTime').val(),
                theme: 'molv',
                type: $('#endTime').attr("data-type") || 'date',
                trigger: 'click',
                done: function(value, date) {
                    // 寮€濮嬫椂闂村皬浜庣粨鏉熸椂闂?
                    if (value !== '') {
                        startLayDate.config.max.year = date.year;
                        startLayDate.config.max.month = date.month - 1;
                        startLayDate.config.max.date = date.date;
                    } else {
                        startLayDate.config.max.year = '2099';
                        startLayDate.config.max.month = '12';
                        startLayDate.config.max.date = '31';
                    }
                }
            });
        });
    }
	
    // laydate time-input 鏃堕棿鎺т欢缁戝畾
    if ($(".time-input").length > 0) {
        layui.use('laydate', function () {
            var com = layui.laydate;
            $(".time-input").each(function (index, item) {
                var time = $(item);
                // 鎺у埗鎺т欢澶栬
                var type = time.attr("data-type") || 'date';
                // 鎺у埗鍥炴樉鏍煎紡
                var format = time.attr("data-format") || 'yyyy-MM-dd';
                // 鎺у埗鏃ユ湡鎺т欢鎸夐挳
                var buttons = time.attr("data-btn") || 'clear|now|confirm', newBtnArr = [];
                // 鏃ユ湡鎺т欢閫夋嫨瀹屾垚鍚庡洖璋冨鐞?
                var callback = time.attr("data-callback") || {};
                if (buttons) {
                    if (buttons.indexOf("|") > 0) {
                        var btnArr = buttons.split("|"), btnLen = btnArr.length;
                        for (var j = 0; j < btnLen; j++) {
                            if ("clear" === btnArr[j] || "now" === btnArr[j] || "confirm" === btnArr[j]) {
                                newBtnArr.push(btnArr[j]);
                            }
                        }
                    } else {
                        if ("clear" === buttons || "now" === buttons || "confirm" === buttons) {
                            newBtnArr.push(buttons);
                        }
                    }
                } else {
                    newBtnArr = ['clear', 'now', 'confirm'];
                }
                com.render({
                    elem: item,
                    theme: 'molv',
                    trigger: 'click',
                    type: type,
                    format: format,
                    btns: newBtnArr,
                    done: function (value, data) {
                        if (typeof window[callback] != 'undefined'
                            && window[callback] instanceof Function) {
                            window[callback](value, data);
                        }
                    }
                });
            });
        });
    }
	
    // tree 鍏抽敭瀛楁悳绱㈢粦瀹?
    if ($("#keyword").length > 0) {
        $("#keyword").bind("focus", function focusKey(e) {
            if ($("#keyword").hasClass("empty")) {
                $("#keyword").removeClass("empty");
            }
        }).bind("blur", function blurKey(e) {
            if ($("#keyword").val() === "") {
                $("#keyword").addClass("empty");
            }
            $.tree.searchNode(e);
        }).bind("input propertychange", $.tree.searchNode);
    }
	
    // tree琛ㄦ牸鏍?灞曞紑/鎶樺彔
    var expandFlag;
    $("#expandAllBtn").click(function() {
        var dataExpand = $.common.isEmpty(table.options.expandAll) ? true : table.options.expandAll;
        expandFlag = $.common.isEmpty(expandFlag) ? dataExpand : expandFlag;
        if (!expandFlag) {
            $.bttTable.bootstrapTreeTable('expandAll');
        } else {
            $.bttTable.bootstrapTreeTable('collapseAll');
        }
        expandFlag = expandFlag ? false: true;
    })
	
    // 鎸変笅ESC鎸夐挳鍏抽棴寮瑰眰
    $('body', document).on('keyup', function(e) {
        if (e.which === 27) {
            $.modal.closeAll();
        }
    });
});

(function ($) {
    'use strict';
    $.fn.toTop = function(opt) {
        var elem = this;
        var win = (opt && opt.hasOwnProperty('win')) ? opt.win : $(window);
        var doc = (opt && opt.hasOwnProperty('doc')) ? opt.doc : $('html, body');
        var options = $.extend({
            autohide: true,
            offset: 50,
            speed: 500,
            position: true,
            right: 15,
            bottom: 5
        }, opt);
        elem.css({
            'cursor': 'pointer'
        });
        if (options.autohide) {
            elem.css('display', 'none');
        }
        if (options.position) {
            elem.css({
                'position': 'fixed',
                'right': options.right,
                'bottom': options.bottom,
            });
        }
        elem.click(function() {
            doc.animate({
                scrollTop: 0
            }, options.speed);
        });
        win.scroll(function() {
            var scrolling = win.scrollTop();
            if (options.autohide) {
                if (scrolling > options.offset) {
                    elem.fadeIn(options.speed);
                } else elem.fadeOut(options.speed);
            }
        });
    };
})(jQuery);

/** 鍒锋柊閫夐」鍗?*/
var refreshItem = function(){
    var topWindow = $(window.parent.document);
    var currentId = $('.page-tabs-content', topWindow).find('.active').attr('data-id');
    var target = $('.coffee_admin_iframe[data-id="' + currentId + '"]', topWindow);
    var url = target.attr('src');
    target.attr('src', url).ready();
}

/** 鍏抽棴閫夐」鍗?*/
var closeItem = function(dataId){
	var topWindow = $(window.parent.document);
	if($.common.isNotEmpty(dataId)){
	    window.parent.$.modal.closeLoading();
	    // 鏍规嵁dataId鍏抽棴鎸囧畾閫夐」鍗?
	    $('.menuTab[data-id="' + dataId + '"]', topWindow).remove();
	    // 绉婚櫎鐩稿簲tab瀵瑰簲鐨勫唴瀹瑰尯
	    $('.mainContent .coffee_admin_iframe[data-id="' + dataId + '"]', topWindow).remove();
	    return;
	}
	var panelUrl = window.frameElement.getAttribute('data-panel');
	$('.page-tabs-content .active i', topWindow).click();
	if($.common.isNotEmpty(panelUrl)){
	    $('.menuTab[data-id="' + panelUrl + '"]', topWindow).addClass('active').siblings('.menuTab').removeClass('active');
	    $('.mainContent .coffee_admin_iframe', topWindow).each(function() {
	        if ($(this).data('id') == panelUrl) {
	            $(this).show().siblings('.coffee_admin_iframe').hide();
	            return false;
            }
        });
    }
}

/** 鍒涘缓閫夐」鍗?*/
function createMenuItem(dataUrl, menuName, isRefresh) {
    var panelUrl = window.frameElement.getAttribute('data-id'),
    dataIndex = $.common.random(1, 100),
    flag = true;
    if (dataUrl == undefined || $.trim(dataUrl).length == 0) return false;
    var topWindow = $(window.parent.document);
    // 閫夐」鍗¤彍鍗曞凡瀛樺湪
    $('.menuTab', topWindow).each(function() {
        if ($(this).data('id') == dataUrl) {
            if (!$(this).hasClass('active')) {
                $(this).addClass('active').siblings('.menuTab').removeClass('active');
                scrollToTab(this);
                $('.page-tabs-content').animate({ marginLeft: ""}, "fast");
                // 鏄剧ずtab瀵瑰簲鐨勫唴瀹瑰尯
                $('.mainContent .coffee_admin_iframe', topWindow).each(function() {
                    if ($(this).data('id') == dataUrl) {
                        $(this).show().siblings('.coffee_admin_iframe').hide();
                        return false;
                    }
                });
            }
            if (isRefresh) {
            	refreshTab();
            }
            flag = false;
            return false;
        }
    });
    // 閫夐」鍗¤彍鍗曚笉瀛樺湪
    if (flag) {
        var str = '<a href="javascript:;" class="active menuTab noactive" data-id="' + dataUrl + '" data-panel="' + panelUrl + '">' + menuName + ' <i class="fa fa-times-circle"></i></a>';
        $('.menuTab', topWindow).removeClass('active');

        // 娣诲姞閫夐」鍗″搴旂殑iframe
        var str1 = '<iframe class="coffee_admin_iframe" name="iframe' + dataIndex + '" width="100%" height="100%" src="' + dataUrl + '" frameborder="0" data-id="' + dataUrl + '" data-panel="' + panelUrl + '" seamless></iframe>';
        $('.mainContent', topWindow).find('iframe.coffee_admin_iframe').hide().parents('.mainContent').append(str1);
        
        window.parent.$.modal.loading("数据加载中，请稍候...");
        $('.mainContent iframe:visible', topWindow).on('load', function() {
            window.parent.$.modal.closeLoading();
        });

        // 娣诲姞閫夐」鍗?
        $('.menuTabs .page-tabs-content', topWindow).append(str);
        scrollToTab($('.menuTab.active', topWindow));
    }
    return false;
}

// 鍒锋柊iframe
function refreshTab() {
	var topWindow = $(window.parent.document);
	var currentId = $('.page-tabs-content', topWindow).find('.active').attr('data-id');
	var target = $('.coffee_admin_iframe[data-id="' + currentId + '"]', topWindow);
    var url = target.attr('src');
	target.attr('src', url).ready();
}

// 婊氬姩鍒版寚瀹氶€夐」鍗?
function scrollToTab(element) {
    var topWindow = $(window.parent.document);
    var marginLeftVal = calSumWidth($(element).prevAll()),
    marginRightVal = calSumWidth($(element).nextAll());
    // 鍙鍖哄煙闈瀟ab瀹藉害
    var tabOuterWidth = calSumWidth($(".content-tabs", topWindow).children().not(".menuTabs"));
    //鍙鍖哄煙tab瀹藉害
    var visibleWidth = $(".content-tabs", topWindow).outerWidth(true) - tabOuterWidth;
    //瀹為檯婊氬姩瀹藉害
    var scrollVal = 0;
    if ($(".page-tabs-content", topWindow).outerWidth() < visibleWidth) {
        scrollVal = 0;
    } else if (marginRightVal <= (visibleWidth - $(element).outerWidth(true) - $(element).next().outerWidth(true))) {
        if ((visibleWidth - $(element).next().outerWidth(true)) > marginRightVal) {
            scrollVal = marginLeftVal;
            var tabElement = element;
            while ((scrollVal - $(tabElement).outerWidth()) > ($(".page-tabs-content", topWindow).outerWidth() - visibleWidth)) {
                scrollVal -= $(tabElement).prev().outerWidth();
                tabElement = $(tabElement).prev();
            }
        }
    } else if (marginLeftVal > (visibleWidth - $(element).outerWidth(true) - $(element).prev().outerWidth(true))) {
        scrollVal = marginLeftVal - $(element).prev().outerWidth(true);
    }
    $('.page-tabs-content', topWindow).animate({ marginLeft: 0 - scrollVal + 'px' }, "fast");
}

// 璁＄畻鍏冪礌闆嗗悎鐨勬€诲搴?
function calSumWidth(elements) {
    var width = 0;
    $(elements).each(function() {
        width += $(this).outerWidth(true);
    });
    return width;
}

// 杩斿洖褰撳墠婵€娲荤殑Tab椤甸潰鍏宠仈鐨刬frame鐨刉indows瀵硅薄
function activeWindow() {
	var topWindow = $(window.parent.document);
	var currentId = $('.page-tabs-content', topWindow).find('.active').attr('data-id');
	if (!currentId) {
		return window.parent;
	}
    return $('.coffee_admin_iframe[data-id="' + currentId + '"]', topWindow)[0].contentWindow;
}

/** 瀵嗙爜瑙勫垯鑼冨洿楠岃瘉 */
function checkpwd(chrtype, password) {
    if (chrtype == 1) {
        if(!$.common.numValid(password)){
            $.modal.alertWarning("密码只能为 0-9 数字");
            return false;
        }
    } else if (chrtype == 2) {
        if(!$.common.enValid(password)){
            $.modal.alertWarning("密码只能为 a-z 和 A-Z 字母");
            return false;
        }
    } else if (chrtype == 3) {
        if(!$.common.enNumValid(password)){
            $.modal.alertWarning("密码必须包含字母和数字");
            return false;
        }
    } else if (chrtype == 4) {
        if(!$.common.charValid(password)){
            $.modal.alertWarning("密码必须包含字母、数字以及特殊符号 <font color='red'>~!@#$%^&*()-=_+</font>");
            return false;
        }
    }
    return true;
}

// 鏃ュ織鎵撳嵃灏佽澶勭悊
var log = {
    log: function(msg) {
        console.log(msg);
    },
    info: function(msg) {
        console.info(msg);
    },
    warn: function(msg) {
        console.warn(msg);
    },
    error: function(msg) {
        console.error(msg);
    }
};

// 鏈湴缂撳瓨澶勭悊
var storage = {
    set: function(key, value) {
        window.localStorage.setItem(key, value);
    },
    get: function(key) {
        return window.localStorage.getItem(key);
    },
    remove: function(key) {
        window.localStorage.removeItem(key);
    },
    clear: function() {
        window.localStorage.clear();
    }
};

// 涓诲瓙琛ㄦ搷浣滃皝瑁呭鐞?
var sub = {
    editRow: function() {
    	var dataColumns = [];
		for (var columnIndex = 0; columnIndex < table.options.columns.length; columnIndex++) {
    		if (table.options.columns[columnIndex].visible != false) {
    			dataColumns.push(table.options.columns[columnIndex]);
    		}
    	}
		var params = new Array();
		var data = $("#" + table.options.id).bootstrapTable('getData');
    	var count = data.length;
    	for (var dataIndex = 0; dataIndex < count; dataIndex++) {
    	    var columns = $('#' + table.options.id + ' tr[data-index="' + dataIndex + '"] td');
    	    var obj = new Object();
    	    for (var i = 0; i < columns.length; i++) {
    	        var inputValue = $(columns[i]).find('input');
    	        var selectValue = $(columns[i]).find('select');
    	        var textareaValue = $(columns[i]).find('textarea');
    	        var key = dataColumns[i].field;
    	        if ($.common.isNotEmpty(inputValue.val())) {
    	            obj[key] = inputValue.val();
    	        } else if ($.common.isNotEmpty(selectValue.val())) {
    	            obj[key] = selectValue.val();
    	        } else if ($.common.isNotEmpty(textareaValue.val())) {
    	            obj[key] = textareaValue.val();
    	        } else {
    	            if (key == "index" && $.common.isNotEmpty(data[dataIndex].index)) {
    	                obj[key] = data[dataIndex].index;
    	            } else {
    	                obj[key] = "";
    	            }
    	        }
    	    }
    	    var item = data[dataIndex];
    	    var extendObj = $.extend({}, item, obj);
    	    params.push({ index: dataIndex, row: extendObj });
    	}
    	$("#" + table.options.id).bootstrapTable("updateRow", params);
    },
    delRow: function(column) {
    	sub.editRow();
    	var subColumn = $.common.isEmpty(column) ? "index" : column;
    	var ids = $.table.selectColumns(subColumn);
        if (ids.length == 0) {
            $.modal.alertWarning("请至少选择一条记录");
            return;
        }
        $("#" + table.options.id).bootstrapTable('remove', { field: subColumn, values: ids });
    },
    delRowByIndex: function(value) {
    	sub.editRow();
        $("#" + table.options.id).bootstrapTable('remove', { field: "index", values: [value] });
        sub.editRow();
    },
    addRow: function(row, tableId) {
    	var currentId = $.common.isEmpty(tableId) ? table.options.id : tableId;
    	table.set(currentId);
    	var count = $("#" + currentId).bootstrapTable('getData').length;
    	sub.editRow();
    	$("#" + currentId).bootstrapTable('insertRow', { index: count + 1, row: row });
    }
};

// 鍔ㄦ€佸姞杞絚ss鏂囦欢
function loadCss(file, headElem) {
    var link = document.createElement('link');
    link.href = file;
    link.rel = 'stylesheet';
    link.type = 'text/css';
    if (headElem) headElem.appendChild(link);
    else document.getElementsByTagName('head')[0].appendChild(link);
}

// 鍔ㄦ€佸姞杞絡s鏂囦欢
function loadJs(file, headElem) {
    var script = document.createElement('script');
    script.src = file;
    script.type = 'text/javascript';
    if (headElem) headElem.appendChild(script);
    else document.getElementsByTagName('head')[0].appendChild(script);
}

// 绂佹鍚庨€€閿紙Backspace锛?
window.onload = function() {
	document.getElementsByTagName("body")[0].onkeydown = function() {
		// 鑾峰彇浜嬩欢瀵硅薄  
		var elem = event.relatedTarget || event.srcElement || event.target || event.currentTarget;
		// 鍒ゆ柇鎸夐敭涓篵ackSpace閿? 
		if (event.keyCode == 8) {
			// 鍒ゆ柇鏄惁闇€瑕侀樆姝㈡寜涓嬮敭鐩樼殑浜嬩欢榛樿浼犻€? 
			var name = elem.nodeName;
			var className = elem.className;
			// 灞忚斀鐗瑰畾鐨勬牱寮忓悕绉?
			if (className.indexOf('note-editable') != -1)
			{
				return true;
			}
			if (name != 'INPUT' && name != 'TEXTAREA') {
				return _stopIt(event);
			}
			var type_e = elem.type.toUpperCase();
			if (name == 'INPUT' && (type_e != 'TEXT' && type_e != 'TEXTAREA' && type_e != 'PASSWORD' && type_e != 'FILE' && type_e != 'SEARCH' && type_e != 'NUMBER' && type_e != 'EMAIL' && type_e != 'URL')) {
				return _stopIt(event);
			}
			if (name == 'INPUT' && (elem.readOnly == true || elem.disabled == true)) {
				return _stopIt(event);
			}
		}
	};
};
function _stopIt(e) {
	if (e.returnValue) {
		e.returnValue = false;
	}
	if (e.preventDefault) {
		e.preventDefault();
	}
	return false;
}

/** 璁剧疆鍏ㄥ眬ajax澶勭悊 */
$.ajaxSetup({
    complete: function(XMLHttpRequest, textStatus) {
        if (textStatus == 'timeout') {
            $.modal.alertWarning("服务器超时，请稍后再试！");
            $.modal.enable();
            $.modal.closeLoading();
        } else if (textStatus == "parsererror" || textStatus == "error") {
            $.modal.alertWarning("服务器错误，请联系管理员");
            $.modal.enable();
            $.modal.closeLoading();
        }
    }
});

