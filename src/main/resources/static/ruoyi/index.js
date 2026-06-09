/**
 * 棣栭〉鏂规硶灏佽澶勭悊
 * Copyright (c) 2026 coffee-mall-admin
 */
layer.config({
    extend: 'moon/style.css',
    skin: 'layer-ext-moon'
});

var isMobile = false;
var sidebarHeight = isMobile ? '100%' : '96%';

$(function() {
    // MetsiMenu
    $('#side-menu').metisMenu();

    // 鍥哄畾鑿滃崟鏍?
    $('.sidebar-collapse').slimScroll({
        height: sidebarHeight,
        railOpacity: 0.9,
        alwaysVisible: false
    });

    // 鑿滃崟鍒囨崲
    $('.navbar-minimalize').click(function() {
    	if (isMobile) {
    	    $("body").toggleClass("canvas-menu");
    	} else {
    	    $("body").toggleClass("mini-navbar");
    	}
        SmoothlyMenu();
    });

    $('#side-menu>li').click(function() {
    	if ($('body').hasClass('canvas-menu mini-navbar')) {
            NavToggle();
        }

    });
    $('#side-menu>li li a:not(:has(span))').click(function() {
        if ($(window).width() < 769) {
            NavToggle();
        }
    });

    $('.nav-close').click(NavToggle);

    //ios娴忚鍣ㄥ吋瀹规€у鐞?
    if (/(iPhone|iPad|iPod|iOS)/i.test(navigator.userAgent)) {
        $('#content-main').css('overflow-y', 'auto');
    }

});

$(window).bind("load resize", function() {
    isMobile = $.common.isMobile() || $(window).width() < 769;
    if (isMobile) {
        $('body').addClass('canvas-menu');
        $("body").removeClass("mini-navbar");
        $("nav .logo").addClass("hide");
        $(".slimScrollDiv").css({ "overflow": "hidden" });
        $('.navbar-static-side').fadeOut();
    } else {
    	if($('body').hasClass('canvas-menu')) {
    	    $('body').addClass('fixed-sidebar');
    	    $('body').removeClass('canvas-menu');
    	    $("body").removeClass("mini-navbar");
    	    $("nav .logo").removeClass("hide");
    	    $(".slimScrollDiv").css({ "overflow": "visible" });
    	    $('.navbar-static-side').fadeIn();
    	}
    }
});

function syncMenuTab(dataId) {
	if(isLinkage) {
        var $dataObj = $('a[href$="' + decodeURI(dataId) + '"]');
        if ($dataObj.attr("class") != null && !$dataObj.hasClass("noactive")) {
            $('.nav ul').removeClass("in");
            $dataObj.parents("ul").addClass("in")
            $dataObj.parents("li").addClass("active").siblings().removeClass("active").find('li').removeClass("active");
            $dataObj.parents("ul").css('height', 'auto').height();
            $(".nav ul li, .nav li").removeClass("selected");
            $dataObj.parent("li").addClass("selected");
            setIframeUrl(dataId);
            
            // 椤堕儴鑿滃崟鍚屾澶勭悊
            var tabStr = $dataObj.parents(".tab-pane").attr("id");
            if ($.common.isNotEmpty(tabStr)) {
                var sepIndex = tabStr.lastIndexOf('_');
                var menuId = tabStr.substring(sepIndex + 1, tabStr.length);
                $("#tab_" + menuId + " a").click();
            }
        }
    }
}

function NavToggle() {
    $('.navbar-minimalize').trigger('click');
}

function fixedSidebar() {
    $('#side-menu').hide();
    $("nav .logo").addClass("hide");
    setTimeout(function() {
        $('#side-menu').fadeIn(500);
    }, 100);
}

// 璁剧疆閿氱偣
function setIframeUrl(href) {
	if($.common.equals("history", mode)) {
	    storage.set('publicPath', href);
	} else {
	    var nowUrl = window.location.href;
	    var newUrl = nowUrl.substring(0, nowUrl.indexOf("#"));
	    window.location.href = newUrl + "#" + href;
	}
}

function SmoothlyMenu() {
    if (isMobile && !$('body').hasClass('canvas-menu')) {
    	$('.navbar-static-side').fadeIn();
    	fixedSidebar();
    } else if (!isMobile &&!$('body').hasClass('mini-navbar')) {
    	fixedSidebar();
    	$("nav .logo").removeClass("hide");
    } else if (isMobile && $('body').hasClass('fixed-sidebar')) {
    	$('.navbar-static-side').fadeOut();
    	fixedSidebar();
    } else if (!isMobile && $('body').hasClass('fixed-sidebar')) {
    	fixedSidebar();
    } else {
        $('#side-menu').removeAttr('style');
    }
}

/**
 * iframe澶勭悊
 */
$(function() {
    //璁＄畻鍏冪礌闆嗗悎鐨勬€诲搴?
    function calSumWidth(elements) {
        var width = 0;
        $(elements).each(function() {
            width += $(this).outerWidth(true);
        });
        return width;
    }

    // 婵€娲绘寚瀹氶€夐」鍗?
    function setActiveTab(element) {
        if (!$(element).hasClass('active')) {
            var currentId = $(element).data('id');
            syncMenuTab(currentId);
            // 鏄剧ずtab瀵瑰簲鐨勫唴瀹瑰尯
            $('.coffee_admin_iframe').each(function() {
                if ($(this).data('id') == currentId) {
                    $(this).show().siblings('.coffee_admin_iframe').hide();
                }
            });
            $(element).addClass('active').siblings('.menuTab').removeClass('active');
            scrollToTab(element);
        }
    }

    //婊氬姩鍒版寚瀹氶€夐」鍗?
    function scrollToTab(element) {
        var marginLeftVal = calSumWidth($(element).prevAll()),
        marginRightVal = calSumWidth($(element).nextAll());
        // 鍙鍖哄煙闈瀟ab瀹藉害
        var tabOuterWidth = calSumWidth($(".content-tabs").children().not(".menuTabs"));
        //鍙鍖哄煙tab瀹藉害
        var visibleWidth = $(".content-tabs").outerWidth(true) - tabOuterWidth;
        //瀹為檯婊氬姩瀹藉害
        var scrollVal = 0;
        if ($(".page-tabs-content").outerWidth() < visibleWidth) {
            scrollVal = 0;
        } else if (marginRightVal <= (visibleWidth - $(element).outerWidth(true) - $(element).next().outerWidth(true))) {
            if ((visibleWidth - $(element).next().outerWidth(true)) > marginRightVal) {
                scrollVal = marginLeftVal;
                var tabElement = element;
                while ((scrollVal - $(tabElement).outerWidth()) > ($(".page-tabs-content").outerWidth() - visibleWidth)) {
                    scrollVal -= $(tabElement).prev().outerWidth();
                    tabElement = $(tabElement).prev();
                }
            }
        } else if (marginLeftVal > (visibleWidth - $(element).outerWidth(true) - $(element).prev().outerWidth(true))) {
            scrollVal = marginLeftVal - $(element).prev().outerWidth(true);
        }
        $('.page-tabs-content').animate({ marginLeft: 0 - scrollVal + 'px' }, "fast");
    }

    //鏌ョ湅宸︿晶闅愯棌鐨勯€夐」鍗?
    function scrollTabLeft() {
        var marginLeftVal = Math.abs(parseInt($('.page-tabs-content').css('margin-left')));
        // 鍙鍖哄煙闈瀟ab瀹藉害
        var tabOuterWidth = calSumWidth($(".content-tabs").children().not(".menuTabs"));
        //鍙鍖哄煙tab瀹藉害
        var visibleWidth = $(".content-tabs").outerWidth(true) - tabOuterWidth;
        //瀹為檯婊氬姩瀹藉害
        var scrollVal = 0;
        if (($(".page-tabs-content").width()) < visibleWidth) {
            return false;
        } else {
            var tabElement = $(".menuTab:first");
            var offsetVal = 0;
            while ((offsetVal + $(tabElement).outerWidth(true)) <= marginLeftVal) { //鎵惧埌绂诲綋鍓峵ab鏈€杩戠殑鍏冪礌
                offsetVal += $(tabElement).outerWidth(true);
                tabElement = $(tabElement).next();
            }
            offsetVal = 0;
            if (calSumWidth($(tabElement).prevAll()) > visibleWidth) {
                while ((offsetVal + $(tabElement).outerWidth(true)) < (visibleWidth) && tabElement.length > 0) {
                    offsetVal += $(tabElement).outerWidth(true);
                    tabElement = $(tabElement).prev();
                }
                scrollVal = calSumWidth($(tabElement).prevAll());
            }
        }
        $('.page-tabs-content').animate({ marginLeft: 0 - scrollVal + 'px' }, "fast");
    }

    //鏌ョ湅鍙充晶闅愯棌鐨勯€夐」鍗?
    function scrollTabRight() {
        var marginLeftVal = Math.abs(parseInt($('.page-tabs-content').css('margin-left')));
        // 鍙鍖哄煙闈瀟ab瀹藉害
        var tabOuterWidth = calSumWidth($(".content-tabs").children().not(".menuTabs"));
        //鍙鍖哄煙tab瀹藉害
        var visibleWidth = $(".content-tabs").outerWidth(true) - tabOuterWidth;
        //瀹為檯婊氬姩瀹藉害
        var scrollVal = 0;
        if ($(".page-tabs-content").width() < visibleWidth) {
            return false;
        } else {
            var tabElement = $(".menuTab:first");
            var offsetVal = 0;
            while ((offsetVal + $(tabElement).outerWidth(true)) <= marginLeftVal) { //鎵惧埌绂诲綋鍓峵ab鏈€杩戠殑鍏冪礌
                offsetVal += $(tabElement).outerWidth(true);
                tabElement = $(tabElement).next();
            }
            offsetVal = 0;
            while ((offsetVal + $(tabElement).outerWidth(true)) < (visibleWidth) && tabElement.length > 0) {
                offsetVal += $(tabElement).outerWidth(true);
                tabElement = $(tabElement).next();
            }
            scrollVal = calSumWidth($(tabElement).prevAll());
            if (scrollVal > 0) {
                $('.page-tabs-content').animate({ marginLeft: 0 - scrollVal + 'px' }, "fast");
            }
        }
    }

    //閫氳繃閬嶅巻缁欒彍鍗曢」鍔犱笂data-index灞炴€?
    $(".menuItem").each(function(index) {
        if (!$(this).attr('data-index')) {
            $(this).attr('data-index', index);
        }
    });

    function menuItem() {
        // 鑾峰彇鏍囪瘑鏁版嵁
        var dataUrl = $(this).attr('href'),
        dataIndex = $(this).data('index'),
        menuName = $.trim($(this).text()),
        isRefresh = $(this).data("refresh"),
        flag = true;

        var $dataObj = $('a[href$="' + decodeURI(dataUrl) + '"]');
        if (!$dataObj.hasClass("noactive")) {
            $('.tab-pane li').removeClass("active");
            $('.nav ul').removeClass("in");
            $dataObj.parents("ul").addClass("in")
            $dataObj.parents("li").addClass("active").siblings().removeClass("active").find('li').removeClass("active");
            $dataObj.parents("ul").css('height', 'auto').height();
            $(".nav ul li, .nav li").removeClass("selected");
            $(this).parent("li").addClass("selected");
        }
        setIframeUrl(dataUrl);
        if (dataUrl == undefined || $.trim(dataUrl).length == 0) return false;

        // 閫夐」鍗¤彍鍗曞凡瀛樺湪
        $('.menuTab').each(function() {
            if ($(this).data('id') == dataUrl) {
                if (!$(this).hasClass('active')) {
                    $(this).addClass('active').siblings('.menuTab').removeClass('active');
                    scrollToTab(this);
                    // 鏄剧ずtab瀵瑰簲鐨勫唴瀹瑰尯
                    $('.mainContent .coffee_admin_iframe').each(function() {
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
            var str = '<a href="javascript:;" class="active menuTab" data-id="' + dataUrl + '">' + menuName + ' <i class="fa fa-times-circle"></i></a>';
            $('.menuTab').removeClass('active');

            // 娣诲姞閫夐」鍗″搴旂殑iframe
            var str1 = '<iframe class="coffee_admin_iframe" name="iframe' + dataIndex + '" width="100%" height="100%" src="' + dataUrl + '" frameborder="0" data-id="' + dataUrl + '" seamless></iframe>';
            $('.mainContent').find('iframe.coffee_admin_iframe').hide().parents('.mainContent').append(str1);

            $.modal.loading("数据加载中，请稍候...");

            $('.mainContent iframe:visible').on('load', function() {
            	$.modal.closeLoading();
            });

            // 娣诲姞閫夐」鍗?
            $('.menuTabs .page-tabs-content').append(str);
            scrollToTab($('.menuTab.active'));
        }
        return false;
    }

    function menuBlank() {
    	// 新窗口打开外链地址，例如 http://example.com
    	var dataUrl = $(this).attr('href');
    	window.open(dataUrl);
    	return false;
    }

    $('.menuItem').on('click', menuItem);

    $('.menuBlank').on('click', menuBlank);

    // 鍏抽棴閫夐」鍗¤彍鍗?
    function closeTab() {
        var closeTabId = $(this).parents('.menuTab').data('id');
        var currentWidth = $(this).parents('.menuTab').width();
        var panelUrl = $(this).parents('.menuTab').data('panel');
        // 褰撳墠鍏冪礌澶勪簬娲诲姩鐘舵€?
        if ($(this).parents('.menuTab').hasClass('active')) {

            // 褰撳墠鍏冪礌鍚庨潰鏈夊悓杈堝厓绱狅紝浣垮悗闈㈢殑涓€涓厓绱犲浜庢椿鍔ㄧ姸鎬?
            if ($(this).parents('.menuTab').next('.menuTab').length) {

                var activeId = $(this).parents('.menuTab').next('.menuTab:eq(0)').data('id');
                $(this).parents('.menuTab').next('.menuTab:eq(0)').addClass('active');

                $('.mainContent .coffee_admin_iframe').each(function() {
                    if ($(this).data('id') == activeId) {
                        $(this).show().siblings('.coffee_admin_iframe').hide();
                        return false;
                    }
                });

                var marginLeftVal = parseInt($('.page-tabs-content').css('margin-left'));
                if (marginLeftVal < 0) {
                    $('.page-tabs-content').animate({ marginLeft: (marginLeftVal + currentWidth) + 'px' }, "fast");
                }

                //  绉婚櫎褰撳墠閫夐」鍗?
                $(this).parents('.menuTab').remove();

                // 绉婚櫎tab瀵瑰簲鐨勫唴瀹瑰尯
                $('.mainContent .coffee_admin_iframe').each(function() {
                    if ($(this).data('id') == closeTabId) {
                        $(this).remove();
                        return false;
                    }
                });
            }

            // 褰撳墠鍏冪礌鍚庨潰娌℃湁鍚岃緢鍏冪礌锛屼娇褰撳墠鍏冪礌鐨勪笂涓€涓厓绱犲浜庢椿鍔ㄧ姸鎬?
            if ($(this).parents('.menuTab').prev('.menuTab').length) {
                var activeId = $(this).parents('.menuTab').prev('.menuTab:last').data('id');
                $(this).parents('.menuTab').prev('.menuTab:last').addClass('active');
                $('.mainContent .coffee_admin_iframe').each(function() {
                    if ($(this).data('id') == activeId) {
                        $(this).show().siblings('.coffee_admin_iframe').hide();
                        return false;
                    }
                });

                //  绉婚櫎褰撳墠閫夐」鍗?
                $(this).parents('.menuTab').remove();

                // 绉婚櫎tab瀵瑰簲鐨勫唴瀹瑰尯
                $('.mainContent .coffee_admin_iframe').each(function() {
                    if ($(this).data('id') == closeTabId) {
                        $(this).remove();
                        return false;
                    }
                });

                if($.common.isNotEmpty(panelUrl)){
            		$('.menuTab[data-id="' + panelUrl + '"]').addClass('active').siblings('.menuTab').removeClass('active');
            		$('.mainContent .coffee_admin_iframe').each(function() {
                        if ($(this).data('id') == panelUrl) {
                            $(this).show().siblings('.coffee_admin_iframe').hide();
                            return false;
                        }
                    });
            	}
            }
        }
        // 褰撳墠鍏冪礌涓嶅浜庢椿鍔ㄧ姸鎬?
        else {
            //  绉婚櫎褰撳墠閫夐」鍗?
            $(this).parents('.menuTab').remove();

            // 绉婚櫎鐩稿簲tab瀵瑰簲鐨勫唴瀹瑰尯
            $('.mainContent .coffee_admin_iframe').each(function() {
                if ($(this).data('id') == closeTabId) {
                    $(this).remove();
                    return false;
                }
            });
        }
        scrollToTab($('.menuTab.active'));
        syncMenuTab($('.page-tabs-content').find('.active').attr('data-id'));
        return false;
    }

    $('.menuTabs').on('click', '.menuTab i', closeTab);

    //婊氬姩鍒板凡婵€娲荤殑閫夐」鍗?
    function showActiveTab() {
        scrollToTab($('.menuTab.active'));
    }
    $('.tabShowActive').on('click', showActiveTab);

    // 鐐瑰嚮閫夐」鍗¤彍鍗?
    function activeTab() {
        if (!$(this).hasClass('active')) {
            var currentId = $(this).data('id');
            syncMenuTab(currentId);
            // 鏄剧ずtab瀵瑰簲鐨勫唴瀹瑰尯
            $('.mainContent .coffee_admin_iframe').each(function() {
                if ($(this).data('id') == currentId) {
                    $(this).show().siblings('.coffee_admin_iframe').hide();
                    return false;
                }
            });
            $(this).addClass('active').siblings('.menuTab').removeClass('active');
            scrollToTab(this);
        }
    }

    // 鐐瑰嚮閫夐」鍗¤彍鍗?
    $('.menuTabs').on('click', '.menuTab', activeTab);

    // 鍒锋柊iframe
    function refreshTab() {
    	var currentId = $('.page-tabs-content').find('.active').attr('data-id');
    	var target = $('.coffee_admin_iframe[data-id="' + currentId + '"]');
        var url = target.attr('src');
    	target.attr('src', url).ready();
    }

    // 椤电鍏ㄥ睆
    function fullScreenTab() {
    	var currentId = $('.page-tabs-content').find('.active').attr('data-id');
    	var target = $('.coffee_admin_iframe[data-id="' + currentId + '"]');
    	target.fullScreen(true);
    }

    // 鍏抽棴褰撳墠閫夐」鍗?
    function tabCloseCurrent() {
    	$('.page-tabs-content').find('.active i').trigger("click");
    }

    //鍏抽棴鍏朵粬閫夐」鍗?
    function tabCloseOther() {
        $('.page-tabs-content').children("[data-id]").not(":first").not(".active").each(function() {
            $('.coffee_admin_iframe[data-id="' + $(this).data('id') + '"]').remove();
            $(this).remove();
        });
        $('.page-tabs-content').animate({ marginLeft: '0px' }, "fast");
    }

    // 鍏抽棴鍏ㄩ儴閫夐」鍗?
    function tabCloseAll() {
    	$('.page-tabs-content').children("[data-id]").not(":first").each(function() {
            $('.coffee_admin_iframe[data-id="' + $(this).data('id') + '"]').remove();
            $(this).remove();
        });
        $('.page-tabs-content').children("[data-id]:first").each(function() {
            $('.coffee_admin_iframe[data-id="' + $(this).data('id') + '"]').show();
            $(this).addClass("active");
        });
        $('.page-tabs-content').css("margin-left", "0");
        syncMenuTab($('.page-tabs-content').find('.active').attr('data-id'));
    }


    // 鍏ㄥ睆鏄剧ず
    $('#fullScreen').on('click', function () {
    	$(document).toggleFullScreen();
    });
    
    // 閿佸畾灞忓箷
    $('#lockScreen').on('click', function () {
    	storage.set('lockPath', $('.page-tabs-content').find('.active').attr('data-id'));
    	location.href  = ctx + "lockscreen";
    });

    // 椤电鍒锋柊鎸夐挳
    $('.tabReload').on('click', refreshTab);

    // 椤电鍏ㄥ睆鎸夐挳
    $('.tabFullScreen').on('click', fullScreenTab);

    // 鍙屽嚮閫夐」鍗″叏灞忔樉绀?
    $('.menuTabs').on('dblclick', '.menuTab', activeTabMax);

    // 宸︾Щ鎸夋壄
    $('.tabLeft').on('click', scrollTabLeft);

    // 鍙崇Щ鎸夋壄
    $('.tabRight').on('click', scrollTabRight);

    // 鍏抽棴褰撳墠
    $('.tabCloseCurrent').on('click', tabCloseCurrent);

    // 鍏抽棴鍏朵粬
    $('.tabCloseOther').on('click', tabCloseOther);

    // 鍏抽棴鍏ㄩ儴
    $('.tabCloseAll').on('click', tabCloseAll);

    // tab鍏ㄥ睆鏄剧ず
    $('.tabMaxCurrent').on('click', function () {
        $('.page-tabs-content').find('.active').trigger("dblclick");
    });

    // 鍏抽棴鍏ㄥ睆
    $('#ax_close_max').click(function(){
    	$('#content-main').toggleClass('max');
    	$('#ax_close_max').hide();
    })

    // 鍙屽嚮閫夐」鍗″叏灞忔樉绀?
    function activeTabMax() {
        $('#content-main').toggleClass('max');
        $('#ax_close_max').show();
    }

    $(window).keydown(function(event) {
        if (event.keyCode == 27) {
            $('#content-main').removeClass('max');
            $('#ax_close_max').hide();
        }
    });

    window.onhashchange = function() {
        var hash = location.hash;
        var url = hash.substring(1, hash.length);
        $('a[href$="' + url + '"]').click();
    };

    // 鍙抽敭鑿滃崟瀹炵幇
    $.contextMenu({
        selector: ".menuTab",
        trigger: 'right',
        autoHide: true,
        items: {
            "close_current": {
                name: "关闭当前",
                icon: "fa-close",
                callback: function(key, opt) {
                    opt.$trigger.find('i').trigger("click");
                }
            },
            "close_other": {
                name: "关闭其他",
                icon: "fa-window-close-o",
                callback: function(key, opt) {
                    setActiveTab(this);
                    tabCloseOther();
                }
            },
            "close_left": {
                name: "关闭左侧",
                icon: "fa-reply",
                callback: function(key, opt) {
                    setActiveTab(this);
                    this.prevAll('.menuTab').not(":last").each(function() {
                        if ($(this).hasClass('active')) {
                            setActiveTab(this);
                        }
                        $('.coffee_admin_iframe[data-id="' + $(this).data('id') + '"]').remove();
                        $(this).remove();
                    });
                    $('.page-tabs-content').animate({ marginLeft: '0px' }, "fast");
                }
            },
            "close_right": {
                name: "关闭右侧",
                icon: "fa-share",
                callback: function(key, opt) {
                    setActiveTab(this);
                    this.nextAll('.menuTab').each(function() {
                        $('.coffee_admin_iframe[data-id="' + $(this).data('id') + '"]').remove();
                        $(this).remove();
                    });
                }
            },
            "close_all": {
                name: "全部关闭",
                icon: "fa-window-close",
                callback: function(key, opt) {
                    tabCloseAll();
                }
            },
            "step": "---------",
            "full": {
                name: "全屏显示",
                icon: "fa-arrows-alt",
                callback: function(key, opt) {
                    setActiveTab(this);
                    var target = $('.coffee_admin_iframe[data-id="' + this.data('id') + '"]');
                    target.fullScreen(true);
                }
            },
            "refresh": {
                name: "刷新页面",
                icon: "fa-refresh",
                callback: function(key, opt) {
                    setActiveTab(this);
                    var target = $('.coffee_admin_iframe[data-id="' + this.data('id') + '"]');
                    var url = target.attr('src');
                    $.modal.loading("数据加载中，请稍候...");
                    target.attr('src', url).on('load', function() {
                    	$.modal.closeLoading();
                    });
                }
            },
            "open": {
                name: "新窗口打开",
                icon: "fa-link",
                callback: function(key, opt) {
                    var target = $('.coffee_admin_iframe[data-id="' + this.data('id') + '"]');
                    window.open(target.attr('src'));
                }
            },
        }
    });
});

