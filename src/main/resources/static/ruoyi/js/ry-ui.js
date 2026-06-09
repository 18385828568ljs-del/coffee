/**
 * 閫氱敤js鏂规硶灏佽澶勭悊
 * Copyright (c) 2026 coffee-mall-admin
 */

// 褰撳墠table鐩稿叧淇℃伅
var table = {
    config: {},
    // 褰撳墠瀹炰緥閰嶇疆
    options: {},
    // 璁剧疆瀹炰緥閰嶇疆
    set: function(id) {
        if($.common.getLength(table.config) > 1 && $.common.isNotEmpty(event)) {
            var tableId = $.common.isEmpty(id) ? $(event.currentTarget).parents(".bootstrap-table").find("table.table").attr("id") : id;
            if ($.common.isNotEmpty(tableId)) {
                table.options = table.get(tableId);
            }
        }
    },
    // 鑾峰彇瀹炰緥閰嶇疆
    get: function(id) {
        return table.config[id];
    },
    // 璁颁綇閫夋嫨瀹炰緥缁?
    rememberSelecteds: {},
    // 璁颁綇閫夋嫨ID缁?
    rememberSelectedIds: {}
};

(function ($) {
    $.extend({
        _tree: {},
        bttTable: {},
        // 琛ㄦ牸灏佽澶勭悊
        table: {
            // 鍒濆鍖栬〃鏍煎弬鏁?
            init: function(options) {
                var defaults = {
                    id: "bootstrap-table",
                    type: 0, // 0 浠ｈ〃bootstrapTable 1浠ｈ〃bootstrapTreeTable
                    method: 'post',
                    height: undefined,
                    sidePagination: "server",
                    sortName: undefined,
                    sortOrder: "asc",
                    pagination: true,
                    paginationLoop: false,
                    pageSize: 10,
                    pageNumber: 1,
                    pageList: [10, 25, 50],
                    toolbar: "toolbar",
                    loadingFontSize: 13,
                    striped: false,
                    escape: false,
                    firstLoad: true,
                    showFooter: false,
                    search: false,
                    showSearch: true,
                    showPageGo: false,
                    showRefresh: true,
                    showColumns: true,
                    showToggle: true,
                    showExport: false,
                    showPrint: false,
                    exportDataType: 'all',
                    exportTypes: ['csv', 'txt', 'doc', 'excel'],
                    clickToSelect: false,
                    singleSelect: false,
                    mobileResponsive: true,
                    maintainSelected: false,
                    rememberSelected: false,
                    fixedColumns: false,
                    fixedNumber: 0,
                    fixedRightNumber: 0,
                    queryParams: $.table.queryParams,
                    rowStyle: undefined
                };
                var options = $.extend(defaults, options);
                table.options = options;
                table.config[options.id] = options;
                $.table.initEvent();
                $('#' + options.id).bootstrapTable({
                    id: options.id,
                    url: options.url,                                   // 璇锋眰鍚庡彴鐨刄RL锛?锛?
                    contentType: "application/x-www-form-urlencoded",   // 缂栫爜绫诲瀷
                    method: options.method,                             // 璇锋眰鏂瑰紡锛?锛?
                    cache: false,                                       // 鏄惁浣跨敤缂撳瓨
                    height: options.height,                             // 琛ㄦ牸鐨勯珮搴?
                    striped: options.striped,                           // 鏄惁鏄剧ず琛岄棿闅旇壊
                    sortable: true,                                     // 鏄惁鍚敤鎺掑簭
                    sortStable: true,                                   // 璁剧疆涓?true 灏嗚幏寰楃ǔ瀹氱殑鎺掑簭
                    sortName: options.sortName,                         // 鎺掑簭鍒楀悕绉?
                    sortOrder: options.sortOrder,                       // 鎺掑簭鏂瑰紡  asc 鎴栬€?desc
                    pagination: options.pagination,                     // 鏄惁鏄剧ず鍒嗛〉锛?锛?
                    paginationLoop: options.paginationLoop,             // 鏄惁鍚敤鍒嗛〉鏉℃棤闄愬惊鐜殑鍔熻兘
                    pageNumber: 1,                                      // 鍒濆鍖栧姞杞界涓€椤碉紝榛樿绗竴椤?
                    pageSize: options.pageSize,                         // 姣忛〉鐨勮褰曡鏁帮紙*锛?
                    pageList: options.pageList,                         // 鍙緵閫夋嫨鐨勬瘡椤电殑琛屾暟锛?锛?
                    firstLoad: options.firstLoad,                       // 鏄惁棣栨璇锋眰鍔犺浇鏁版嵁锛屽浜庢暟鎹緝澶у彲浠ラ厤缃甪alse
                    escape: options.escape,                             // 杞箟HTML瀛楃涓?
                    showFooter: options.showFooter,                     // 鏄惁鏄剧ず琛ㄥ熬
                    iconSize: 'outline',                                // 鍥炬爣澶у皬锛歶ndefined榛樿鐨勬寜閽昂瀵?xs瓒呭皬鎸夐挳sm灏忔寜閽甽g澶ф寜閽?
                    toolbar: '#' + options.toolbar,                     // 鎸囧畾宸ヤ綔鏍?
                    loadingFontSize: options.loadingFontSize,           // 鑷畾涔夊姞杞芥枃鏈殑瀛椾綋澶у皬
                    sidePagination: options.sidePagination,             // server鍚敤鏈嶅姟绔垎椤礳lient瀹㈡埛绔垎椤?
                    search: options.search,                             // 鏄惁鏄剧ず鎼滅储妗嗗姛鑳?
                    searchText: options.searchText,                     // 鎼滅储妗嗗垵濮嬫樉绀虹殑鍐呭锛岄粯璁や负绌?
                    showSearch: options.showSearch,                     // 鏄惁鏄剧ず妫€绱俊鎭?
                    showPageGo: options.showPageGo,                     // 鏄惁鏄剧ず璺宠浆椤?
                    showRefresh: options.showRefresh,                   // 鏄惁鏄剧ず鍒锋柊鎸夐挳
                    showColumns: options.showColumns,                   // 鏄惁鏄剧ず闅愯棌鏌愬垪涓嬫媺妗?
                    showToggle: options.showToggle,                     // 鏄惁鏄剧ず璇︾粏瑙嗗浘鍜屽垪琛ㄨ鍥剧殑鍒囨崲鎸夐挳
                    showExport: options.showExport,                     // 鏄惁鏀寔瀵煎嚭鏂囦欢
                    showPrint: options.showPrint,                       // 鏄惁鏀寔鎵撳嵃椤甸潰
                    showHeader: options.showHeader,                     // 鏄惁鏄剧ず琛ㄥご
                    showFullscreen: options.showFullscreen,             // 鏄惁鏄剧ず鍏ㄥ睆鎸夐挳
                    uniqueId: options.uniqueId,                         // 鍞竴鐨勬爣璇嗙
                    clickToSelect: options.clickToSelect,               // 鏄惁鍚敤鐐瑰嚮閫変腑琛?
                    singleSelect: options.singleSelect,                 // 鏄惁鍗曢€塩heckbox
                    mobileResponsive: options.mobileResponsive,         // 鏄惁鏀寔绉诲姩绔€傞厤
                    cardView: options.cardView,                         // 鏄惁鍚敤鏄剧ず鍗＄墖瑙嗗浘
                    detailView: options.detailView,                     // 鏄惁鍚敤鏄剧ず缁嗚妭瑙嗗浘
                    onCheck: options.onCheck,                           // 褰撻€夋嫨姝よ鏃惰Е鍙?
                    onUncheck: options.onUncheck,                       // 褰撳彇娑堟琛屾椂瑙﹀彂
                    onCheckAll: options.onCheckAll,                     // 褰撳叏閫夎鏃惰Е鍙?
                    onUncheckAll: options.onUncheckAll,                 // 褰撳彇娑堝叏閫夎鏃惰Е鍙?
                    onClickRow: options.onClickRow,                     // 鐐瑰嚮鏌愯瑙﹀彂鐨勪簨浠?
                    onDblClickRow: options.onDblClickRow,               // 鍙屽嚮鏌愯瑙﹀彂鐨勪簨浠?
                    onClickCell: options.onClickCell,                   // 鍗曞嚮鏌愭牸瑙﹀彂鐨勪簨浠?
                    onDblClickCell: options.onDblClickCell,             // 鍙屽嚮鏌愭牸瑙﹀彂鐨勪簨浠?
                    onEditableSave: options.onEditableSave,             // 琛屽唴缂栬緫淇濆瓨鐨勪簨浠?
                    onExpandRow: options.onExpandRow,                   // 鐐瑰嚮璇︾粏瑙嗗浘鐨勪簨浠?
                    onPostBody: options.onPostBody,                     // 娓叉煋瀹屾垚鍚庢墽琛岀殑浜嬩欢
                    maintainSelected: options.maintainSelected,         // 鍓嶇缈婚〉鏃朵繚鐣欐墍閫夎
                    rememberSelected: options.rememberSelected,         // 鍚敤缈婚〉璁颁綇鍓嶉潰鐨勯€夋嫨
                    fixedColumns: options.fixedColumns,                 // 鏄惁鍚敤鍐荤粨鍒楋紙宸︿晶锛?
                    fixedNumber: options.fixedNumber,                   // 鍒楀喕缁撶殑涓暟锛堝乏渚э級
                    fixedRightNumber: options.fixedRightNumber,         // 鍒楀喕缁撶殑涓暟锛堝彸渚э級
                    onReorderRow: options.onReorderRow,                 // 褰撴嫋鎷界粨鏉熷悗澶勭悊鍑芥暟
                    queryParams: options.queryParams,                   // 浼犻€掑弬鏁帮紙*锛?
                    rowStyle: options.rowStyle,                         // 閫氳繃鑷畾涔夊嚱鏁拌缃鏍峰紡
                    footerStyle: options.footerStyle,                   // 閫氳繃鑷畾涔夊嚱鏁拌缃〉鑴氭牱寮?
                    headerStyle: options.headerStyle,                   // 閫氳繃鑷畾涔夊嚱鏁拌缃爣棰樻牱寮?
                    columns: options.columns,                           // 鏄剧ず鍒椾俊鎭紙*锛?
                    data: options.data,                                 // 琚姞杞界殑鏁版嵁
                    responseHandler: $.table.responseHandler,           // 鍦ㄥ姞杞芥湇鍔″櫒鍙戦€佹潵鐨勬暟鎹箣鍓嶅鐞嗗嚱鏁?
                    onLoadSuccess: $.table.onLoadSuccess,               // 褰撴墍鏈夋暟鎹鍔犺浇鏃惰Е鍙戝鐞嗗嚱鏁?
                    exportOptions: options.exportOptions,               // 鍓嶇瀵煎嚭蹇界暐鍒楃储寮?
                    exportDataType: options.exportDataType,             // 瀵煎嚭鏂瑰紡锛堥粯璁ll锛氬鍑烘墍鏈夋暟鎹紱basic锛氬鍑哄綋鍓嶉〉鐨勬暟鎹紱selected锛氬鍑洪€変腑鐨勬暟鎹級
                    exportTypes: options.exportTypes,                   // 瀵煎嚭鏂囦欢绫诲瀷 锛坖son銆亁ml銆乸ng銆乧sv銆乼xt銆乻ql銆乨oc銆乪xcel銆亁lsx銆乸owerpoint銆乸df锛?
                    printPageBuilder: options.printPageBuilder,         // 鑷畾涔夋墦鍗伴〉闈㈡ā鏉?
                    detailFormatter: options.detailFormatter,           // 鍦ㄨ涓嬮潰灞曠ず鍏朵粬鏁版嵁鍒楄〃
                });
            },
            // 鑾峰彇瀹炰緥ID锛屽瀛樺湪澶氫釜杩斿洖#id1,#id2 delimeter鍒嗛殧绗?
            getOptionsIds: function(separator) {
                var _separator = $.common.isEmpty(separator) ? "," : separator;
                var optionsIds = "";  
                $.each(table.config, function(key, value){
                    optionsIds += "#" + key + _separator;
                });
                return optionsIds.substring(0, optionsIds.length - 1);
            },
            // 鏌ヨ鏉′欢
            queryParams: function(params) {
                table.set();
                var curParams = {
                    // 浼犻€掑弬鏁版煡璇㈠弬鏁?
                    pageSize:       params.limit,
                    pageNum:        params.offset / params.limit + 1,
                    searchValue:    params.search,
                    orderByColumn:  params.sort,
                    isAsc:          params.order
                };
                var currentId = $.common.isEmpty(table.options.formId) ? $('form').attr('id') : table.options.formId;
                return $.extend(curParams, $.common.formToJSON(currentId)); 
            },
            // 璇锋眰鑾峰彇鏁版嵁鍚庡鐞嗗洖璋冨嚱鏁?
            responseHandler: function(res) {
                if (typeof table.get(this.id).responseHandler == "function") {
                    table.get(this.id).responseHandler(res);
                }
                var thisOptions = table.config[this.id];
                if (res.code == web_status.SUCCESS) {
                    if ($.common.isNotEmpty(thisOptions.sidePagination) && thisOptions.sidePagination == 'client') {
                        return res.rows;
                    } else {
                        if ($.common.isNotEmpty(thisOptions.rememberSelected) && thisOptions.rememberSelected) {
                            var column = $.common.isEmpty(thisOptions.uniqueId) ? thisOptions.columns[1].field : thisOptions.uniqueId;
                            $.each(res.rows, function(i, row) {
                                row.state = $.inArray(row[column], table.rememberSelectedIds[thisOptions.id]) !== -1;
                            })
                        }
                        return { rows: res.rows, total: res.total };
                    }
                } else {
                    $.modal.alertWarning(res.msg);
                    return { rows: [], total: 0 };
                }
            },
            // 鍒濆鍖栦簨浠?
            initEvent: function() {
                // 瀹炰緥ID淇℃伅
                var optionsIds = $.table.getOptionsIds();
                // 鐩戝惉浜嬩欢澶勭悊
                $(optionsIds).on(TABLE_EVENTS, function () {
                    table.set($(this).attr("id"));
                });
                // 鍦ㄨ〃鏍间綋娓叉煋瀹屾垚锛屽苟鍦?DOM 涓彲瑙佸悗瑙﹀彂锛堜簨浠讹級
                $(optionsIds).on("post-body.bs.table", function (e, args) {
                    // 娴姩鎻愮ず妗嗙壒鏁?
                    $(".table [data-toggle='tooltip']").tooltip();
                    // 姘旀场寮瑰嚭妗嗙壒鏁?
                    $('.table [data-toggle="popover"]').popover();
                });
                // 閫変腑銆佸彇娑堛€佸叏閮ㄩ€変腑銆佸叏閮ㄥ彇娑堬紙浜嬩欢锛?
                $(optionsIds).on("check.bs.table check-all.bs.table uncheck.bs.table uncheck-all.bs.table", function (e, rowsAfter, rowsBefore) {
                    // 澶嶉€夋鍒嗛〉淇濈暀淇濆瓨閫変腑鏁扮粍
                    var rows = $.common.equals("uncheck-all", e.type) ? rowsBefore : rowsAfter;
                    var rowIds = $.table.affectedRowIds(rows);
                    if ($.common.isNotEmpty(table.options.rememberSelected) && table.options.rememberSelected) {
                        func = $.inArray(e.type, ['check', 'check-all']) > -1 ? 'union' : 'difference';
                        var selectedIds = table.rememberSelectedIds[table.options.id];
                        if($.common.isNotEmpty(selectedIds)) {
                            table.rememberSelectedIds[table.options.id] = _[func](selectedIds, rowIds);
                        } else {
                            table.rememberSelectedIds[table.options.id] = _[func]([], rowIds);
                        }
                        var selectedRows = table.rememberSelecteds[table.options.id];
                        if($.common.isNotEmpty(selectedRows)) {
                            table.rememberSelecteds[table.options.id] = _[func](selectedRows, rows);
                        } else {
                            table.rememberSelecteds[table.options.id] = _[func]([], rows);
                        }
                    }
                });
                // 鍔犺浇鎴愬姛銆侀€変腑銆佸彇娑堛€佸叏閮ㄩ€変腑銆佸叏閮ㄥ彇娑堬紙浜嬩欢锛?
                $(optionsIds).on("check.bs.table uncheck.bs.table check-all.bs.table uncheck-all.bs.table load-success.bs.table", function () {
                    var toolbar = table.options.toolbar;
                    var uniqueId = table.options.uniqueId;
                    // 宸ュ叿鏍忔寜閽帶鍒?
                    var rows = $.common.isEmpty(uniqueId) ? $.table.selectFirstColumns() : $.table.selectColumns(uniqueId);
                    // 闈炲涓鐢?
                    $('#' + toolbar + ' .multiple').toggleClass('disabled', !rows.length);
                    // 闈炲崟涓鐢?
                    $('#' + toolbar + ' .single').toggleClass('disabled', rows.length!=1);
                });
                // 鍥剧墖棰勮浜嬩欢
                $(optionsIds).off("click").on("click", '.img-circle', function() {
                    var src = $(this).attr('src');
                    var target = $(this).data('target');
                    if($.common.equals("self", target)) {
                        var height = $(this).data('height');
                        var width = $(this).data('width');
                        top.layer.open({
                            title: false,
                            type: 1,
                            closeBtn: true,
                            shadeClose: true,
                            area: ['auto', 'auto'],
                            content: "<img src='" + src + "' height='" + height + "' width='" + width + "'/>"
                        });
                    } else if ($.common.equals("blank", target)) {
                        window.open(src);
                    }
                });
                // 鍗曞嚮tooltip浜嬩欢
                $(optionsIds).on("click", '.tooltip-show', function() {
                    var target = $(this).data('target');
                    var input = $(this).prev();
                    if ($.common.equals("copy", target)) {
                        input.select();
                        document.execCommand("copy");
                    } else if ($.common.equals("open", target)) {
                        top.layer.alert(input.val(), {
                            title: "信息内容",
                            shadeClose: true,
                            btn: ['确认'],
                            btnclass: ['btn btn-primary'],
                        });
                    }
                });
            },
            // 褰撴墍鏈夋暟鎹鍔犺浇鏃惰Е鍙?
            onLoadSuccess: function(data) {
                if (typeof table.options.onLoadSuccess == "function") {
                    table.options.onLoadSuccess(data);
                }
            },
            // 琛ㄦ牸閿€姣?
            destroy: function (tableId) {
                var currentId = $.common.isEmpty(tableId) ? table.options.id : tableId;
                $("#" + currentId).bootstrapTable('destroy');
                delete table.rememberSelectedIds[currentId];
                delete table.rememberSelecteds[currentId];
            },
            // 搴忓垪鍙风敓鎴?
            serialNumber: function (index, tableId) {
                var currentId = $.common.isEmpty(tableId) ? table.options.id : tableId;
                var tableParams = $("#" + currentId).bootstrapTable('getOptions');
                var pageSize = $.common.isNotEmpty(tableParams.pageSize) ? tableParams.pageSize: table.options.pageSize;
                var pageNumber = $.common.isNotEmpty(tableParams.pageNumber) ? tableParams.pageNumber: table.options.pageNumber;
                if (table.options.sidePagination == 'client') {
                    return index + 1;
                }
                return pageSize * (pageNumber - 1) + index + 1;
            },
            // 鍒楄秴鍑烘寚瀹氶暱搴︽诞鍔ㄦ彁绀?target锛坈opy鍗曞嚮澶嶅埗鏂囨湰 open寮圭獥鎵撳紑鏂囨湰锛?
            tooltip: function (value, length, target) {
                var _length = $.common.isEmpty(length) ? 20 : length;
                var _text = "";
                var _value = $.common.nullToStr(value);
                var _target = $.common.isEmpty(target) ? 'copy' : target;
                if (_value.length > _length) {
                    _text = _value.substr(0, _length) + "...";
                    _value = _value.replace(/\'/g,"&apos;");
                    _value = _value.replace(/\"/g,"&quot;");
                    var actions = [];
                    actions.push($.common.sprintf('<input style="opacity: 0;position: absolute;width:5px;z-index:-1" type="text" value="%s"/>', _value));
                    actions.push($.common.sprintf('<a href="###" class="tooltip-show" data-toggle="tooltip" data-target="%s" title="%s">%s</a>', _target, _value, _text));
                    return actions.join('');
                } else {
                    _text = _value;
                    return _text;
                }
            },
            // 涓嬫媺鎸夐挳鍒囨崲
            dropdownToggle: function (value) {
                var actions = [];
                actions.push('<div class="btn-group">');
                actions.push('<button type="button" class="btn btn-xs dropdown-toggle" data-toggle="dropdown" aria-expanded="false">');
                actions.push('<i class="fa fa-cog"></i>&nbsp;<span class="fa fa-chevron-down"></span></button>');
                actions.push('<ul class="dropdown-menu">');
                actions.push(value.replace(/<a/g,"<li><a").replace(/<\/a>/g,"</a></li>"));
                actions.push('</ul>');
                actions.push('</div>');
                return actions.join('');
            },
            // 鍥剧墖棰勮
            imageView: function (value, height, width, target) {
                if ($.common.isEmpty(width)) {
                    width = 'auto';
                }
                if ($.common.isEmpty(height)) {
                    height = 'auto';
                }
                // blank or self
                var _target = $.common.isEmpty(target) ? 'self' : target;
                if ($.common.isNotEmpty(value)) {
                    return $.common.sprintf("<img class='img-circle img-xs' data-height='%s' data-width='%s' data-target='%s' src='%s'/>", height, width, _target, value);
                } else {
                    return $.common.nullToStr(value);
                }
            },
            // 鎼滅储-榛樿绗竴涓猣orm
            search: function(formId, tableId, pageNumber, pageSize) {
                table.set(tableId);
                table.options.formId = $.common.isEmpty(formId) ? $('form').attr('id') : formId;
                var params = $.common.isEmpty(tableId) ? $("#" + table.options.id).bootstrapTable('getOptions') : $("#" + tableId).bootstrapTable('getOptions');
                if ($.common.isNotEmpty(pageNumber)) {
                    params.pageNumber = pageNumber;
                }
                if ($.common.isNotEmpty(pageSize)) {
                    params.pageSize = pageSize;
                }
                if($.common.isNotEmpty(tableId)){
                    $("#" + tableId).bootstrapTable('refresh', params);
                } else{
                    $("#" + table.options.id).bootstrapTable('refresh', params);
                }
            },
            // 瀵煎嚭鏁版嵁
            exportExcel: function(formId) {
                table.set();
            $.modal.confirm("确定导出所有" + table.options.modalName + "吗？", function() {
                    var currentId = $.common.isEmpty(formId) ? $('form').attr('id') : formId;
                    var params = $("#" + table.options.id).bootstrapTable('getOptions');
                    var dataParam = $("#" + currentId).serializeArray();
                    dataParam.push({ "name": "orderByColumn", "value": params.sortName });
                    dataParam.push({ "name": "isAsc", "value": params.sortOrder });
                    $.modal.loading("姝ｅ湪瀵煎嚭鏁版嵁锛岃绋嶅€?..");
                    $.post(table.options.exportUrl, dataParam, function(result) {
                        if (result.code == web_status.SUCCESS) {
                            window.location.href = ctx + "common/download?fileName=" + encodeURI(result.msg) + "&delete=" + true;
                        } else if (result.code == web_status.WARNING) {
                            $.modal.alertWarning(result.msg)
                        } else {
                            $.modal.alertError(result.msg);
                        }
                        $.modal.closeLoading();
                    });
                });
            },
            // 涓嬭浇妯℃澘
            importTemplate: function() {
                $.get(activeWindow().table.options.importTemplateUrl, function(result) {
                    if (result.code == web_status.SUCCESS) {
                        window.location.href = ctx + "common/download?fileName=" + encodeURI(result.msg) + "&delete=" + true;
                    } else if (result.code == web_status.WARNING) {
                        $.modal.alertWarning(result.msg)
                    } else {
                        $.modal.alertError(result.msg);
                    }
                });
            },
            // 瀵煎叆鏁版嵁
            importExcel: function(formId, width, height) {
                table.set();
                var currentId = $.common.isEmpty(formId) ? 'importTpl' : formId;
                var _width = $.common.isEmpty(width) ? "400" : width;
                var _height = $.common.isEmpty(height) ? "230" : height;
                top.layer.open({
                    type: 1,
                    area: [_width + 'px', _height + 'px'],
                    fix: false,
                    //涓嶅浐瀹?
                    maxmin: true,
                    shade: 0.3,
                    title: '导入' + table.options.modalName + '数据',
                    content: $('#' + currentId).html(),
                    btn: ['<i class="fa fa-check"></i> 导入', '<i class="fa fa-remove"></i> 取消'],
                    // 寮瑰眰澶栧尯鍩熷叧闂?
                    shadeClose: true,
                    btn1: function(index, layero){
                        var file = layero.find('#file').val();
                        if (file == '' || (!$.common.endWith(file, '.xls') && !$.common.endWith(file, '.xlsx'))){
                $.modal.msgWarning("请选择后缀为“xls”或“xlsx”的文件。");
                            return false;
                        }
                        var index = top.layer.load(2, {shade: false});
                        $.modal.disable();
                        var formData = new FormData(layero.find('form')[0]);
                        $.ajax({
                            url: table.options.importUrl,
                            data: formData,
                            cache: false,
                            contentType: false,
                            processData: false,
                            type: 'POST',
                            success: function (result) {
                                if (result.code == web_status.SUCCESS) {
                                	$.modal.close(index);
                                    $.modal.closeAll();
                                    $.modal.alertSuccess(result.msg);
                                    $.table.refresh();
                                } else if (result.code == web_status.WARNING) {
                                	$.modal.close(index);
                                    $.modal.enable();
                                    $.modal.alertWarning(result.msg)
                                } else {
                                    $.modal.close(index);
                                    $.modal.enable();
                                    $.modal.alertError(result.msg);
                                }
                            },
                            complete: function () {
                            	layero.find('#file').val('');
                            }
                        });
                    }
                });
            },
            // 鍒锋柊琛ㄦ牸
            refresh: function(tableId, pageNumber, pageSize, url) {
                var currentId = $.common.isEmpty(tableId) ? table.options.id : tableId;
                var params = $("#" + currentId).bootstrapTable('getOptions');
                if ($.common.isEmpty(pageNumber)) {
                    pageNumber = params.pageNumber;
                }
                if ($.common.isEmpty(pageSize)) {
                    pageSize = params.pageSize;
                }
                if ($.common.isEmpty(url)) {
                    url = $.common.isEmpty(url) ? params.url : url;
                }
                $("#" + currentId).bootstrapTable('refresh', {
                    silent: true,
                    url: url,
                    pageNumber: pageNumber,
                    pageSize: pageSize
                });
            },
            // 鍒锋柊options閰嶇疆
            refreshOptions: function(options, tableId) {
                var currentId = $.common.isEmpty(tableId) ? table.options.id : tableId;
                $("#" + currentId).bootstrapTable('refreshOptions', options);
            },
            // 鏌ヨ琛ㄦ牸鎸囧畾鍒楀€?deDuplication锛?true鍘婚噸銆乫alse涓嶅幓閲嶏級
            selectColumns: function(column, deDuplication) {
                var distinct = $.common.isEmpty(deDuplication) ? true : deDuplication;
                var rows = $.map($("#" + table.options.id).bootstrapTable('getSelections'), function (row) {
                    return $.common.getItemField(row, column);
                });
                if ($.common.isNotEmpty(table.options.rememberSelected) && table.options.rememberSelected) {
                    var selectedRows = table.rememberSelecteds[table.options.id];
                    if($.common.isNotEmpty(selectedRows)) {
                        rows = $.map(table.rememberSelecteds[table.options.id], function (row) {
                            return $.common.getItemField(row, column);
                        });
                    }
                }
                return distinct ? $.common.uniqueFn(rows) : rows;
            },
            // 鑾峰彇褰撳墠椤甸€変腑鎴栬€呭彇娑堢殑琛孖D
            affectedRowIds: function(rows) {
                var column = $.common.isEmpty(table.options.uniqueId) ? table.options.columns[1].field : table.options.uniqueId;
                var rowIds;
                if ($.isArray(rows)) {
                    rowIds = $.map(rows, function(row) {
                        return $.common.getItemField(row, column);
                    });
                } else {
                    rowIds = [rows[column]];
                }
                return rowIds;
            },
            // 鏌ヨ琛ㄦ牸棣栧垪鍊糳eDuplication锛?true鍘婚噸銆乫alse涓嶅幓閲嶏級
            selectFirstColumns: function(deDuplication) {
                var distinct = $.common.isEmpty(deDuplication) ? true : deDuplication;
                var rows = $.map($("#" + table.options.id).bootstrapTable('getSelections'), function (row) {
                    return $.common.getItemField(row, table.options.columns[1].field);
                });
                if ($.common.isNotEmpty(table.options.rememberSelected) && table.options.rememberSelected) {
                    var selectedRows = table.rememberSelecteds[table.options.id];
                    if($.common.isNotEmpty(selectedRows)) {
                        rows = $.map(selectedRows, function (row) {
                            return $.common.getItemField(row, table.options.columns[1].field);
                        });
                    }
                }
                return distinct ? $.common.uniqueFn(rows) : rows;
            },
            // 鍥炴樉鏁版嵁瀛楀吀
            selectDictLabel: function(datas, value) {
                if ($.common.isEmpty(datas) || $.common.isEmpty(value)) {
                    return '';
                }
                var actions = [];
                $.each(datas, function(index, dict) {
                    if (dict.dictValue == ('' + value)) {
                        var listClass = $.common.equals("default", dict.listClass) || $.common.isEmpty(dict.listClass) ? "" : "badge badge-" + dict.listClass;
                        actions.push($.common.sprintf("<span class='%s'>%s</span>", listClass, dict.dictLabel));
                        return false;
                    }
                });
                if (actions.length === 0) {
                    actions.push($.common.sprintf("<span>%s</span>", value))
                }
                return actions.join('');
            },
            // 鍥炴樉鏁版嵁瀛楀吀锛堝瓧绗︿覆鏁扮粍锛?
            selectDictLabels: function(datas, value, separator) {
                if ($.common.isEmpty(datas) || $.common.isEmpty(value)) {
                    return '';
                }
                var currentSeparator = $.common.isEmpty(separator) ? "," : separator;
                var actions = [];
                $.each(value.split(currentSeparator), function(i, val) {
                    var match = false
                    $.each(datas, function(index, dict) {
                        if (dict.dictValue == ('' + val)) {
                            var listClass = $.common.equals("default", dict.listClass) || $.common.isEmpty(dict.listClass) ? "" : "badge badge-" + dict.listClass;
                            actions.push($.common.sprintf("<span class='%s'>%s</span>", listClass, dict.dictLabel));
                            match = true
                            return false;
                        }
                    });
                    if (!match) {
                        actions.push($.common.sprintf("<span> %s </span>", val));
                    }
                });
                return actions.join('');
            },
            // 鏄剧ず琛ㄦ牸鎸囧畾鍒?
            showColumn: function(column, tableId) {
                var currentId = $.common.isEmpty(tableId) ? table.options.id : tableId;
                $("#" + currentId).bootstrapTable('showColumn', column);
            },
            // 闅愯棌琛ㄦ牸鎸囧畾鍒?
            hideColumn: function(column, tableId) {
                var currentId = $.common.isEmpty(tableId) ? table.options.id : tableId;
                $("#" + currentId).bootstrapTable('hideColumn', column);
            },
            // 鏄剧ず鎵€鏈夎〃鏍煎垪
            showAllColumns: function(tableId) {
                var currentId = $.common.isEmpty(tableId) ? table.options.id : tableId;
                $("#" + currentId).bootstrapTable('showAllColumns');
            },
            // 闅愯棌鎵€鏈夎〃鏍煎垪
            hideAllColumns: function(tableId) {
                var currentId = $.common.isEmpty(tableId) ? table.options.id : tableId;
                $("#" + currentId).bootstrapTable('hideAllColumns');
            }
        },
        // 琛ㄦ牸鏍戝皝瑁呭鐞?
        treeTable: {
            // 鍒濆鍖栬〃鏍?
            init: function(options) {
                var defaults = {
                    id: "bootstrap-tree-table",
                    type: 1, // 0 浠ｈ〃bootstrapTable 1浠ｈ〃bootstrapTreeTable
                    height: 0,
                    rootIdValue: 0,
                    ajaxParams: {},
                    toolbar: "toolbar",
                    striped: false,
                    pagination: false,
                    pageSize: 10,
                    pageList: [10, 25, 50],
                    expandColumn: 1,
                    showSearch: true,
                    showRefresh: true,
                    showColumns: true,
                    expandAll: true,
                    expandFirst: true
                };
                var options = $.extend(defaults, options);
                table.options = options;
                table.config[options.id] = options;
                $.table.initEvent();
                $.bttTable = $('#' + options.id).bootstrapTreeTable({
                    code: options.code,                                 // 鐢ㄤ簬璁剧疆鐖跺瓙鍏崇郴
                    parentCode: options.parentCode,                     // 鐢ㄤ簬璁剧疆鐖跺瓙鍏崇郴
                    type: 'post',                                       // 璇锋眰鏂瑰紡锛?锛?
                    url: options.url,                                   // 璇锋眰鍚庡彴鐨刄RL锛?锛?
                    data: options.data,                                 // 鏃爑rl鏃剁敤浜庢覆鏌撶殑鏁版嵁
                    ajaxParams: options.ajaxParams,                     // 璇锋眰鏁版嵁鐨刟jax鐨刣ata灞炴€?
                    rootIdValue: options.rootIdValue,                   // 璁剧疆鎸囧畾鏍硅妭鐐筰d鍊?
                    height: options.height,                             // 琛ㄦ牸鏍戠殑楂樺害
                    pagination: options.pagination,                     // 鏄惁鏄剧ず鍒嗛〉
                    dataUrl: options.dataUrl,                           // 鍔犺浇瀛愯妭鐐瑰紓姝ヨ姹傛暟鎹畊rl
                    pageSize: options.pageSize,                         // 姣忛〉鐨勮褰曡鏁?
                    pageList: options.pageList,                         // 鍙緵閫夋嫨鐨勬瘡椤电殑琛屾暟
                    expandColumn: options.expandColumn,                 // 鍦ㄥ摢涓€鍒椾笂闈㈡樉绀哄睍寮€鎸夐挳
                    striped: options.striped,                           // 鏄惁鏄剧ず琛岄棿闅旇壊
                    bordered: options.bordered,                         // 鏄惁鏄剧ず杈规
                    toolbar: '#' + options.toolbar,                     // 鎸囧畾宸ヤ綔鏍?
                    showSearch: options.showSearch,                     // 鏄惁鏄剧ず妫€绱俊鎭?
                    showRefresh: options.showRefresh,                   // 鏄惁鏄剧ず鍒锋柊鎸夐挳
                    showColumns: options.showColumns,                   // 鏄惁鏄剧ず闅愯棌鏌愬垪涓嬫媺妗?
                    expandAll: options.expandAll,                       // 鏄惁鍏ㄩ儴灞曞紑
                    expandFirst: options.expandFirst,                   // 鏄惁榛樿绗竴绾у睍寮€--expandAll涓篺alse鏃剁敓鏁?
                    columns: options.columns,                           // 鏄剧ず鍒椾俊鎭紙*锛?
                    onClickRow: options.onClickRow,                     // 鍗曞嚮鏌愯浜嬩欢
                    responseHandler: $.treeTable.responseHandler,       // 鍦ㄥ姞杞芥湇鍔″櫒鍙戦€佹潵鐨勬暟鎹箣鍓嶅鐞嗗嚱鏁?
                    onLoadSuccess: $.treeTable.onLoadSuccess            // 褰撴墍鏈夋暟鎹鍔犺浇鏃惰Е鍙戝鐞嗗嚱鏁?
                });
            },
            // 鏉′欢鏌ヨ
            search: function(formId) {
                var currentId = $.common.isEmpty(formId) ? $('form').attr('id') : formId;
                var params = $.common.formToJSON(currentId);
                $.bttTable.bootstrapTreeTable('refresh', $.extend(params, table.options.ajaxParams));
            },
            // 鍒锋柊
            refresh: function() {
                $.bttTable.bootstrapTreeTable('refresh');
            },
            // 鏌ヨ琛ㄦ牸鏍戞寚瀹氬垪鍊糳eDuplication锛?true鍘婚噸銆乫alse涓嶅幓閲嶏級
            selectColumns: function(column, deDuplication) {
                var distinct = $.common.isEmpty(deDuplication) ? true : deDuplication;
                var rows = $.map($.bttTable.bootstrapTreeTable('getSelections'), function (row) {
                    return $.common.getItemField(row, column);
                });
                return distinct ? $.common.uniqueFn(rows) : rows;
            },
            // 璇锋眰鑾峰彇鏁版嵁鍚庡鐞嗗洖璋冨嚱鏁帮紝鏍￠獙寮傚父鐘舵€佹彁閱?
            responseHandler: function(res) {
                if (typeof table.options.responseHandler == "function") {
                    table.options.responseHandler(res);
                }
                if (res.code != undefined && res.code != web_status.SUCCESS) {
                    $.modal.alertWarning(res.msg);
                    return [];
                } else {
                    return res;
                }
            },
            // 褰撴墍鏈夋暟鎹鍔犺浇鏃惰Е鍙?
            onLoadSuccess: function(data) {
                if (typeof table.options.onLoadSuccess == "function") {
                    table.options.onLoadSuccess(data);
                }
                $(".table [data-toggle='tooltip']").tooltip();
            },
        },
        // 琛ㄥ崟灏佽澶勭悊
        form: {
            // 琛ㄥ崟閲嶇疆
            reset: function(formId, tableId, pageNumber, pageSize) {
                table.set(tableId);
                formId = $.common.isEmpty(formId) ? $('form').attr('id') : formId;
                $("#" + formId)[0].reset();
                var tableId = $.common.isEmpty(tableId) ? table.options.id : tableId;
                if (table.options.type == table_type.bootstrapTable) {
                    var params = $("#" + tableId).bootstrapTable('getOptions');
                    if ($.common.isNotEmpty(pageNumber)) {
                        params.pageNumber = pageNumber;
                    }
                    if ($.common.isNotEmpty(pageSize)) {
                        params.pageSize = pageSize;
                    }
                    $("#" + tableId).bootstrapTable('refresh', params);
                } else if (table.options.type == table_type.bootstrapTreeTable) {
                    $("#" + tableId).bootstrapTreeTable('refresh', table.options.ajaxParams);
                }
                if ($.common.isNotEmpty(startLayDate) && $.common.isNotEmpty(endLayDate)) {
                    endLayDate.config.min.year = '';
                    endLayDate.config.min.month = '';
                    endLayDate.config.min.date = '';
                    startLayDate.config.max.year = '2099';
                    startLayDate.config.max.month = '12';
                    startLayDate.config.max.date = '31';
                 }
            },
            // 鑾峰彇閫変腑澶嶉€夋椤?
            selectCheckeds: function(name) {
                var checkeds = "";
                $('input:checkbox[name="' + name + '"]:checked').each(function(i) {
                    if (0 == i) {
                        checkeds = $(this).val();
                    } else {
                        checkeds += ("," + $(this).val());
                    }
                });
                return checkeds;
            },
            // 鑾峰彇閫変腑涓嬫媺妗嗛」
            selectSelects: function(name) {
                var selects = "";
                $('#' + name + ' option:selected').each(function (i) {
                    if (0 == i) {
                        selects = $(this).val();
                    } else {
                        selects += ("," + $(this).val());
                    }
                });
                return selects;
            }
        },
        // 寮瑰嚭灞傚皝瑁呭鐞?
        modal: {
            // 鏄剧ず鍥炬爣
            icon: function(type) {
                var icon = "";
                if (type == modal_status.WARNING) {
                    icon = 0;
                } else if (type == modal_status.SUCCESS) {
                    icon = 1;
                } else if (type == modal_status.FAIL) {
                    icon = 2;
                } else {
                    icon = 3;
                }
                return icon;
            },
            // 娑堟伅鎻愮ず
            msg: function(content, type) {
                if (type != undefined) {
                	top.layer.msg(content, { icon: $.modal.icon(type), time: 1000, shift: 5 });
                } else {
                	top.layer.msg(content);
                }
            },
            // 閿欒娑堟伅
            msgError: function(content) {
                $.modal.msg(content, modal_status.FAIL);
            },
            // 鎴愬姛娑堟伅
            msgSuccess: function(content) {
                $.modal.msg(content, modal_status.SUCCESS);
            },
            // 璀﹀憡娑堟伅
            msgWarning: function(content) {
                $.modal.msg(content, modal_status.WARNING);
            },
            // 寮瑰嚭鎻愮ず
            alert: function(content, type) {
                top.layer.alert(content, {
                    icon: $.modal.icon(type),
                    title: "系统提示",
                    btn: ['确认'],
                    btnclass: ['btn btn-primary'],
                });
            },
            // 閿欒鎻愮ず
            alertError: function(content) {
                $.modal.alert(content, modal_status.FAIL);
            },
            // 鎴愬姛鎻愮ず
            alertSuccess: function(content) {
                $.modal.alert(content, modal_status.SUCCESS);
            },
            // 璀﹀憡鎻愮ず
            alertWarning: function(content) {
                $.modal.alert(content, modal_status.WARNING);
            },
            // 娑堟伅鎻愮ず锛岄噸鏂板姞杞介〉闈?
            msgReload: function(msg, type) {
                top.layer.msg(msg, {
                    icon: $.modal.icon(type),
                    time: 500,
                    shade: [0.1, '#8F8F8F']
                },
                function() {
                    $.modal.reload();
                });
            },
            // 娑堟伅鎻愮ず鎴愬姛骞跺埛鏂扮埗绐椾綋
            msgSuccessReload: function(msg) {
            	$.modal.msgReload(msg, modal_status.SUCCESS);
            },
            // 鑾峰彇iframe椤电殑DOM
            getChildFrame: function (index) {
                if($.common.isEmpty(index)){
                    var index = parent.layer.getFrameIndex(window.name);
                    return parent.layer.getChildFrame('body', index);
                } else {
                    return top.layer.getChildFrame('body', index);
                }
            },
            // 鍏抽棴绐椾綋
            close: function (index) {
                if($.common.isEmpty(index)){
                    var index = parent.layer.getFrameIndex(window.name);
                    parent.layer.close(index);
                } else {
                    top.layer.close(index);
                }
            },
            // 鍏抽棴鍏ㄩ儴绐椾綋
            closeAll: function () {
                top.layer.closeAll();
            },
            // 纭绐椾綋
            confirm: function (content, callBack) {
                top.layer.confirm(content, {
                    icon: 3,
                    title: "系统提示",
                    btn: ['确认', '取消']
                }, function (index) {
                    $.modal.close(index);
                    callBack(true);
                });
            },
            // 寮瑰嚭灞傛寚瀹氬搴?
            open: function (title, url, width, height, callback) {
                // 濡傛灉鏄Щ鍔ㄧ锛屽氨浣跨敤鑷€傚簲澶у皬寮圭獥
                if ($.common.isMobile()) {
                    width = 'auto';
                    height = 'auto';
                }
                if ($.common.isEmpty(title)) {
                    title = false;
                }
                if ($.common.isEmpty(url)) {
                    url = "/404.html";
                }
                if ($.common.isEmpty(width)) {
                    width = 800;
                }
                if ($.common.isEmpty(height)) {
                    height = ($(window).height() - 50);
                }
                if ($.common.isEmpty(callback)) {
                    callback = function(index, layero) {
                        var iframeWin = layero.find('iframe')[0];
                        iframeWin.contentWindow.submitHandler(index, layero);
                    }
                }
                top.layer.open({
                    type: 2,
                    area: [width + 'px', height + 'px'],
                    fix: false,
                    //涓嶅浐瀹?
                    maxmin: true,
                    shade: 0.3,
                    title: title,
                    content: url,
                    btn: ['确定', '关闭'],
                    // 寮瑰眰澶栧尯鍩熷叧闂?
                    shadeClose: true,
                    yes: callback,
                    cancel: function(index) {
                        return true;
                    }
                });
            },
            // 寮瑰嚭灞傛寚瀹氬弬鏁伴€夐」
            openOptions: function (options) {
                var _url = $.common.isEmpty(options.url) ? "/404.html" : options.url; 
                var _title = $.common.isEmpty(options.title) ? "系统窗口" : options.title; 
                var _width = $.common.isEmpty(options.width) ? "800" : options.width; 
                var _height = $.common.isEmpty(options.height) ? ($(window).height() - 50) : options.height;
                var _btn = ['<i class="fa fa-check"></i> 确认', '<i class="fa fa-close"></i> 关闭'];
                // 濡傛灉鏄Щ鍔ㄧ锛屽氨浣跨敤鑷€傚簲澶у皬寮圭獥
                if ($.common.isMobile()) {
                    _width = 'auto';
                    _height = 'auto';
                }
                if ($.common.isEmpty(options.yes)) {
                    options.yes = function(index, layero) {
                        options.callBack(index, layero);
                    }
                }
                var btnCallback = {};
                if(options.btn instanceof Array){
                    for (var i = 1, len = options.btn.length; i < len; i++) {
                        var btn = options["btn" + (i + 1)];
                        if (btn) {
                            btnCallback["btn" + (i + 1)] = btn;
                        }
                    }
                }
                var index = top.layer.open($.extend({
                    id: options.id,       // 鍞竴id
                    anim: options.anim,   // 寮瑰嚭鍔ㄧ敾 0-6
                    type: 2,
                    maxmin: $.common.isEmpty(options.maxmin) ? true : options.maxmin,
                    shade: 0.3,
                    title: _title,
                    fix: false,
                    area: [_width + 'px', _height + 'px'],
                    content: _url,
                    shadeClose: $.common.isEmpty(options.shadeClose) ? true : options.shadeClose,
                    skin: options.skin,
                    // options.btn璁剧疆涓?琛ㄧず涓嶆樉绀烘寜閽?
                    btn: $.common.isEmpty(options.btn) ? _btn : options.btn,
                    yes: options.yes,
                    cancel: function () {
                        return true;
                    }
                }, btnCallback));
                if ($.common.isNotEmpty(options.full) && options.full === true) {
                    top.layer.full(index);
                }
            },
            // 寮瑰嚭灞傚叏灞?
            openFull: function (title, url, width, height) {
                // 濡傛灉鏄Щ鍔ㄧ锛屽氨浣跨敤鑷€傚簲澶у皬寮圭獥
                if ($.common.isMobile()) {
                    width = 'auto';
                    height = 'auto';
                }
                if ($.common.isEmpty(title)) {
                    title = false;
                }
                if ($.common.isEmpty(url)) {
                    url = "/404.html";
                }
                if ($.common.isEmpty(width)) {
                    width = 800;
                }
                if ($.common.isEmpty(height)) {
                    height = ($(window).height() - 50);
                }
                var index = top.layer.open({
                    type: 2,
                    area: [width + 'px', height + 'px'],
                    fix: false,
                    //涓嶅浐瀹?
                    maxmin: true,
                    shade: 0.3,
                    title: title,
                    content: url,
                    btn: ['确定', '关闭'],
                    // 寮瑰眰澶栧尯鍩熷叧闂?
                    shadeClose: true,
                    yes: function(index, layero) {
                        var iframeWin = layero.find('iframe')[0];
                        iframeWin.contentWindow.submitHandler(index, layero);
                    },
                    cancel: function(index) {
                        return true;
                    }
                });
                top.layer.full(index);
            },
            // 閫夊崱椤垫柟寮忔墦寮€
            openTab: function (title, url, isRefresh) {
                createMenuItem(url, title, isRefresh);
            },
            // 閫夊崱椤靛悓涓€椤电鎵撳紑
            parentTab: function (title, url) {
                var dataId = window.frameElement.getAttribute('data-id');
                createMenuItem(url, title);
                closeItem(dataId);
            },
            // 鍏抽棴閫夐」鍗?
            closeTab: function (dataId) {
                closeItem(dataId);
            },
            // 绂佺敤鎸夐挳
            disable: function() {
                var doc = window.top == window.parent ? window.document : window.parent.document;
                $("a[class*=layui-layer-btn]", doc).addClass("layer-disabled");
            },
            // 鍚敤鎸夐挳
            enable: function() {
                var doc = window.top == window.parent ? window.document : window.parent.document;
                $("a[class*=layui-layer-btn]", doc).removeClass("layer-disabled");
            },
            // 鎵撳紑閬僵灞?
            loading: function (message) {
                $.blockUI({ message: '<div class="loaderbox"><div class="loading-activity"></div> ' + message + '</div>' });
            },
            // 鍏抽棴閬僵灞?
            closeLoading: function () {
                setTimeout(function(){
                    $.unblockUI();
                }, 50);
            },
            // 閲嶆柊鍔犺浇
            reload: function () {
                parent.location.reload();
            }
        },
        // 鎿嶄綔灏佽澶勭悊
        operate: {
            // 鎻愪氦鏁版嵁
            submit: function(url, type, dataType, data, callback) {
                var config = {
                    url: url,
                    type: type,
                    dataType: dataType,
                    data: data,
                    beforeSend: function () {
                        $.modal.loading("正在处理中，请稍候...");
                    },
                    success: function(result) {
                        if (typeof callback == "function") {
                            callback(result);
                        }
                        $.operate.ajaxSuccess(result);
                    }
                };
                $.ajax(config)
            },
            // post璇锋眰浼犺緭
            post: function(url, data, callback) {
                $.operate.submit(url, "post", "json", data, callback);
            },
            // get璇锋眰浼犺緭
            get: function(url, callback) {
                $.operate.submit(url, "get", "json", "", callback);
            },
            // 璇︾粏淇℃伅
            detail: function(id, width, height) {
                table.set();
                var _url = $.operate.detailUrl(id);
                var options = {
                    title: table.options.modalName + "详情",
                    width: width,
                    height: height,
                    url: _url,
                    skin: 'layui-layer-gray', 
                    btn: ['关闭'],
                    yes: function (index, layero) {
                        $.modal.close(index);
                    }
                };
                $.modal.openOptions(options);
            },
            // 璇︾粏淇℃伅锛屼互tab椤靛睍鐜?
            detailTab: function(id) {
                table.set();
                $.modal.openTab("详情" + table.options.modalName, $.operate.detailUrl(id));
            },
            // 璇︾粏璁块棶鍦板潃
            detailUrl: function(id) {
                var url = "/404.html";
                if ($.common.isNotEmpty(id)) {
                    url = table.options.detailUrl.replace("{id}", id);
                } else {
                    var id = $.common.isEmpty(table.options.uniqueId) ? $.table.selectFirstColumns() : $.table.selectColumns(table.options.uniqueId);
                    if (id.length == 0) {
                $.modal.alertWarning("请至少选择一条记录");
                        return;
                    }
                    url = table.options.detailUrl.replace("{id}", id);
                }
                return url;
            },
            // 鍒犻櫎淇℃伅
            remove: function(id) {
                table.set();
                $.modal.confirm("确定删除这条" + table.options.modalName + "信息吗？", function() {
                    var url = $.common.isEmpty(id) ? table.options.removeUrl : table.options.removeUrl.replace("{id}", id);
                    if(table.options.type == table_type.bootstrapTreeTable) {
                        $.operate.get(url);
                    } else {
                        var data = { "ids": id };
                        $.operate.submit(url, "post", "json", data);
                    }
                });
            },
            // 鎵归噺鍒犻櫎淇℃伅
            removeAll: function() {
                table.set();
                var rows = $.common.isEmpty(table.options.uniqueId) ? $.table.selectFirstColumns() : $.table.selectColumns(table.options.uniqueId);
                if (rows.length == 0) {
                $.modal.alertWarning("请至少选择一条记录");
                    return;
                }
                $.modal.confirm("确认要删除选中的" + rows.length + "条数据吗？", function() {
                    var url = table.options.removeUrl;
                    var data = { "ids": rows.join() };
                    $.operate.submit(url, "post", "json", data);
                });
            },
            // 娓呯┖淇℃伅
            clean: function() {
                table.set();
            $.modal.confirm("确定清空所有" + table.options.modalName + "吗？", function() {
                    var url = table.options.cleanUrl;
                    $.operate.submit(url, "post", "json", "");
                });
            },
            // 娣诲姞淇℃伅
            add: function(id) {
                table.set();
                $.modal.open("添加" + table.options.modalName, $.operate.addUrl(id));
            },
            // 娣诲姞淇℃伅锛屼互tab椤靛睍鐜?
            addTab: function (id) {
                table.set();
                $.modal.openTab("添加" + table.options.modalName, $.operate.addUrl(id));
            },
            // 娣诲姞淇℃伅 鍏ㄥ睆
            addFull: function(id) {
                table.set();
                $.modal.openFull("添加" + table.options.modalName, $.operate.addUrl(id));
            },
            // 娣诲姞璁块棶鍦板潃
            addUrl: function(id) {
                var url = $.common.isEmpty(id) ? table.options.createUrl.replace("{id}", "") : table.options.createUrl.replace("{id}", id);
                return url;
            },
            // 淇敼淇℃伅
            edit: function(id) {
                table.set();
                if($.common.isEmpty(id) && table.options.type == table_type.bootstrapTreeTable) {
                    var row = $("#" + table.options.id).bootstrapTreeTable('getSelections')[0];
                    if ($.common.isEmpty(row)) {
                $.modal.alertWarning("请至少选择一条记录");
                        return;
                    }
                    var url = table.options.updateUrl.replace("{id}", row[table.options.uniqueId]);
                    $.modal.open("修改" + table.options.modalName, url);
                } else {
                    $.modal.open("修改" + table.options.modalName, $.operate.editUrl(id));
                }
            },
            // 淇敼淇℃伅锛屼互tab椤靛睍鐜?
            editTab: function(id) {
                table.set();
                $.modal.openTab("修改" + table.options.modalName, $.operate.editUrl(id));
            },
            // 淇敼淇℃伅 鍏ㄥ睆
            editFull: function(id) {
                table.set();
                var url = "/404.html";
                if ($.common.isNotEmpty(id)) {
                    url = table.options.updateUrl.replace("{id}", id);
                } else {
                    if(table.options.type == table_type.bootstrapTreeTable) {
                        var row = $("#" + table.options.id).bootstrapTreeTable('getSelections')[0];
                        if ($.common.isEmpty(row)) {
                $.modal.alertWarning("请至少选择一条记录");
                            return;
                        }
                        url = table.options.updateUrl.replace("{id}", row[table.options.uniqueId]);
                    } else {
                        var row = $.common.isEmpty(table.options.uniqueId) ? $.table.selectFirstColumns() : $.table.selectColumns(table.options.uniqueId);
                        url = table.options.updateUrl.replace("{id}", row);
                    }
                }
                $.modal.openFull("修改" + table.options.modalName, url);
            },
            // 淇敼璁块棶鍦板潃
            editUrl: function(id) {
                var url = "/404.html";
                if ($.common.isNotEmpty(id)) {
                    url = table.options.updateUrl.replace("{id}", id);
                } else {
                    var id = $.common.isEmpty(table.options.uniqueId) ? $.table.selectFirstColumns() : $.table.selectColumns(table.options.uniqueId);
                    if (id.length == 0) {
                $.modal.alertWarning("请至少选择一条记录");
                        return;
                    }
                    url = table.options.updateUrl.replace("{id}", id);
                }
                return url;
            },
            // 淇濆瓨淇℃伅 鍒锋柊琛ㄦ牸
            save: function(url, data, callback) {
                var config = {
                    url: url,
                    type: "post",
                    dataType: "json",
                    data: data,
                    beforeSend: function () {
                        $.modal.loading("正在处理中，请稍候...");
                        $.modal.disable();
                    },
                    success: function(result) {
                        if (typeof callback == "function") {
                            callback(result);
                        }
                        $.operate.successCallback(result);
                    }
                };
                $.ajax(config)
            },
            // 淇濆瓨淇℃伅 寮瑰嚭缁撴灉鎻愮ず妗?
            saveModal: function(url, data, callback) {
                var config = {
                    url: url,
                    type: "post",
                    dataType: "json",
                    data: data,
                    beforeSend: function () {
                        $.modal.loading("正在处理中，请稍候...");
                    },
                    success: function(result) {
                        if (typeof callback == "function") {
                            callback(result);
                        }
                        if (result.code == web_status.SUCCESS) {
                            $.modal.alertSuccess(result.msg)
                        } else if (result.code == web_status.WARNING) {
                            $.modal.alertWarning(result.msg)
                        } else {
                            $.modal.alertError(result.msg);
                        }
                        $.modal.closeLoading();
                    }
                };
                $.ajax(config)
            },
            // 淇濆瓨閫夐」鍗′俊鎭?
            saveTab: function(url, data, callback) {
                var config = {
                    url: url,
                    type: "post",
                    dataType: "json",
                    data: data,
                    beforeSend: function () {
                        $.modal.loading("正在处理中，请稍候...");
                    },
                    success: function(result) {
                        if (typeof callback == "function") {
                            callback(result);
                        }
                        $.operate.successTabCallback(result);
                    }
                };
                $.ajax(config)
            },
            // 淇濆瓨缁撴灉寮瑰嚭msg鍒锋柊table琛ㄦ牸
            ajaxSuccess: function (result) {
                if (result.code == web_status.SUCCESS && table.options.type == table_type.bootstrapTable) {
                    $.modal.msgSuccess(result.msg);
                    $.table.refresh();
                } else if (result.code == web_status.SUCCESS && table.options.type == table_type.bootstrapTreeTable) {
                    $.modal.msgSuccess(result.msg);
                    $.treeTable.refresh();
                } else if (result.code == web_status.SUCCESS && $.common.isEmpty(table.options.type)) {
                    $.modal.msgSuccess(result.msg)
                }  else if (result.code == web_status.WARNING) {
                    $.modal.alertWarning(result.msg)
                }  else {
                    $.modal.alertError(result.msg);
                }
                $.modal.closeLoading();
            },
            // 淇濆瓨缁撴灉閲嶆柊鍔犺浇椤甸潰
            saveReload: function (result) {
                if (result.code == web_status.SUCCESS) {
                    $.modal.msgSuccessReload(result.msg);
                } else if (result.code == web_status.WARNING) {
                    $.modal.alertWarning(result.msg)
                }  else {
                    $.modal.alertError(result.msg);
                }
                $.modal.closeLoading();
            },
            // 鎴愬姛鍥炶皟鎵ц浜嬩欢锛堢埗绐椾綋闈欓粯鏇存柊锛?
            successCallback: function(result) {
                if (result.code == web_status.SUCCESS) {
                    var parent = activeWindow();
                    if($.common.isEmpty(parent.table)) {
                    	$.modal.msgSuccessReload(result.msg);
                    } else if (parent.table.options.type == table_type.bootstrapTable) {
                        $.modal.close();
                        parent.$.modal.msgSuccess(result.msg);
                        parent.$.table.refresh();
                    } else if (parent.table.options.type == table_type.bootstrapTreeTable) {
                        $.modal.close();
                        parent.$.modal.msgSuccess(result.msg);
                        parent.$.treeTable.refresh();
                    }
                } else if (result.code == web_status.WARNING) {
                    $.modal.alertWarning(result.msg)
                }  else {
                    $.modal.alertError(result.msg);
                }
                $.modal.closeLoading();
                $.modal.enable();
            },
            // 閫夐」鍗℃垚鍔熷洖璋冩墽琛屼簨浠讹紙鐖剁獥浣撻潤榛樻洿鏂帮級
            successTabCallback: function(result) {
                if (result.code == web_status.SUCCESS) {
                    var topWindow = $(window.parent.document);
                    var currentId = $('.page-tabs-content', topWindow).find('.active').attr('data-panel');
                    var $contentWindow = $('.coffee_admin_iframe[data-id="' + currentId + '"]', topWindow)[0].contentWindow;
                    $.modal.close();
                    $contentWindow.$.modal.msgSuccess(result.msg);
                    $contentWindow.$(".layui-layer-padding").removeAttr("style");
                    if ($contentWindow.table.options.type == table_type.bootstrapTable) {
                        $contentWindow.$.table.refresh();
                    } else if ($contentWindow.table.options.type == table_type.bootstrapTreeTable) {
                        $contentWindow.$.treeTable.refresh();
                    }
                    $.modal.closeTab();
                } else if (result.code == web_status.WARNING) {
                    $.modal.alertWarning(result.msg)
                } else {
                    $.modal.alertError(result.msg);
                }
                $.modal.closeLoading();
            }
        },
        // 鏍￠獙灏佽澶勭悊
        validate: {
            // 鍒ゆ柇杩斿洖鏍囪瘑鏄惁鍞竴 false 涓哄瓨鍦?true 涓轰笉瀛樺湪
            unique: function (value) {
                if (value == "0") {
                    return true;
                }
                return false;
            },
            // 琛ㄥ崟楠岃瘉
            form: function (formId) {
                var currentId = $.common.isEmpty(formId) ? $('form').attr('id') : formId;
                return $("#" + currentId).validate().form();
            },
            // 閲嶇疆琛ㄥ崟楠岃瘉锛堟竻闄ゆ彁绀轰俊鎭級
            reset: function (formId) {
                var currentId = $.common.isEmpty(formId) ? $('form').attr('id') : formId;
                return $("#" + currentId).validate().resetForm();
            }
        },
        // 鏍戞彃浠跺皝瑁呭鐞?
        tree: {
            _option: {},
            _lastValue: {},
            // 鍒濆鍖栨爲缁撴瀯
            init: function(options) {
                var defaults = {
                    id: "tree",                    // 灞炴€D
                    expandLevel: 0,                // 灞曞紑绛夌骇鑺傜偣
                    view: {
                        selectedMulti: false,      // 璁剧疆鏄惁鍏佽鍚屾椂閫変腑澶氫釜鑺傜偣
                        nameIsHTML: true           // 璁剧疆 name 灞炴€ф槸鍚︽敮鎸?HTML 鑴氭湰
                    },
                    check: {
                        enable: false,             // 缃?zTree 鐨勮妭鐐逛笂鏄惁鏄剧ず checkbox / radio
                        nocheckInherit: true,      // 璁剧疆瀛愯妭鐐规槸鍚﹁嚜鍔ㄧ户鎵?
                        chkboxType: { "Y": "ps", "N": "ps" } // 鐖跺瓙鑺傜偣鐨勫叧鑱斿叧绯?
                    },
                    data: {
                        key: {
                            title: "title"         // 鑺傜偣鏁版嵁淇濆瓨鑺傜偣鎻愮ず淇℃伅鐨勫睘鎬у悕绉?
                        },
                        simpleData: {
                            enable: true           // true / false 鍒嗗埆琛ㄧず 浣跨敤 / 涓嶄娇鐢?绠€鍗曟暟鎹ā寮?
                        }
                    },
                };
                var options = $.extend(defaults, options);
                $.tree._option = options;
                // 鏍戠粨鏋勫垵濮嬪寲鍔犺浇
                var setting = {
                    callback: {
                        onClick: options.onClick,                      // 鐢ㄤ簬鎹曡幏鑺傜偣琚偣鍑荤殑浜嬩欢鍥炶皟鍑芥暟
                        onCheck: options.onCheck,                      // 鐢ㄤ簬鎹曡幏 checkbox / radio 琚嬀閫?鎴?鍙栨秷鍕鹃€夌殑浜嬩欢鍥炶皟鍑芥暟
                        onDblClick: options.onDblClick                 // 鐢ㄤ簬鎹曡幏榧犳爣鍙屽嚮涔嬪悗鐨勪簨浠跺洖璋冨嚱鏁?
                    },
                    check: options.check,
                    view: options.view,
                    data: options.data
                };
                $.get(options.url, function(data) {
                    var treeId = $("#treeId").val();
                    tree = $.fn.zTree.init($("#" + options.id), setting, data);
                    $._tree = tree;
                    for (var i = 0; i < options.expandLevel; i++) {
                        var nodes = tree.getNodesByParam("level", i);
                        for (var j = 0; j < nodes.length; j++) {
                            tree.expandNode(nodes[j], true, false, false);
                        }
                    }
                    var node = tree.getNodesByParam("id", treeId, null)[0];
                    $.tree.selectByIdName(treeId, node);
                    // 鍥炶皟tree鏂规硶
                    if(typeof(options.callBack) === "function"){
                        options.callBack(tree);
                    }
                });
            },
            // 鎼滅储鑺傜偣
            searchNode: function() {
                // 鍙栧緱杈撳叆鐨勫叧閿瓧鐨勫€?
                var value = $.common.trim($("#keyword").val());
                if ($.tree._lastValue == value) {
                    return;
                }
                // 淇濆瓨鏈€鍚庝竴娆℃悳绱㈠悕绉?
                $.tree._lastValue = value;
                var nodes = $._tree.getNodes();
                // 濡傛灉瑕佹煡绌哄瓧涓诧紝灏遍€€鍑轰笉鏌ヤ簡銆?
                if (value == "") {
                    $.tree.showAllNode(nodes);
                    return;
                }
                $.tree.hideAllNode(nodes);
                // 鏍规嵁鎼滅储鍊兼ā绯婂尮閰?
                $.tree.updateNodes($._tree.getNodesByParamFuzzy("name", value));
            },
            // 鏍规嵁Id鍜孨ame閫変腑鎸囧畾鑺傜偣
            selectByIdName: function(treeId, node) {
                if ($.common.isNotEmpty(treeId) && node && treeId == node.id) {
                    $._tree.selectNode(node, true);
                }
            },
            // 鏄剧ず鎵€鏈夎妭鐐?
            showAllNode: function(nodes) {
                nodes = $._tree.transformToArray(nodes);
                for (var i = nodes.length - 1; i >= 0; i--) {
                    if (nodes[i].getParentNode() != null) {
                        $._tree.expandNode(nodes[i], true, false, false, false);
                    } else {
                        $._tree.expandNode(nodes[i], true, true, false, false);
                    }
                    $._tree.showNode(nodes[i]);
                    $.tree.showAllNode(nodes[i].children);
                }
            },
            // 闅愯棌鎵€鏈夎妭鐐?
            hideAllNode: function(nodes) {
                var nodes = $._tree.transformToArray(nodes);
                for (var i = nodes.length - 1; i >= 0; i--) {
                    $._tree.hideNode(nodes[i]);
                }
            },
            // 鏄剧ず鎵€鏈夌埗鑺傜偣
            showParent: function(treeNode) {
                var parentNode;
                while ((parentNode = treeNode.getParentNode()) != null) {
                    $._tree.showNode(parentNode);
                    $._tree.expandNode(parentNode, true, false, false);
                    treeNode = parentNode;
                }
            },
            // 鏄剧ず鎵€鏈夊瀛愯妭鐐?
            showChildren: function(treeNode) {
                if (treeNode.isParent) {
                    for (var idx in treeNode.children) {
                        var node = treeNode.children[idx];
                        $._tree.showNode(node);
                        $.tree.showChildren(node);
                    }
                }
            },
            // 鏇存柊鑺傜偣鐘舵€?
            updateNodes: function(nodeList) {
                $._tree.showNodes(nodeList);
                for (var i = 0, l = nodeList.length; i < l; i++) {
                    var treeNode = nodeList[i];
                    $.tree.showChildren(treeNode);
                    $.tree.showParent(treeNode)
                }
            },
            // 鑾峰彇褰撳墠琚嬀閫夐泦鍚?
            getCheckedNodes: function(column) {
                var _column = $.common.isEmpty(column) ? "id" : column;
                var nodes = $._tree.getCheckedNodes(true);
                return $.map(nodes, function (row) {
                    return row[_column];
                }).join();
            },
            // 涓嶅厑璁告牴鐖惰妭鐐归€夋嫨
            notAllowParents: function(_tree) {
                var nodes = _tree.getSelectedNodes();
                if(nodes.length == 0){
            $.modal.msgError("请选择节点后提交");
                    return false;
                }
                for (var i = 0; i < nodes.length; i++) {
                    if (nodes[i].level == 0) {
                $.modal.msgError("不能选择根节点（" + nodes[i].name + "）");
                        return false;
                    }
                    if (nodes[i].isParent) {
                $.modal.msgError("不能选择父节点（" + nodes[i].name + "）");
                        return false;
                    }
                }
                return true;
            },
            // 涓嶅厑璁告渶鍚庡眰绾ц妭鐐归€夋嫨
            notAllowLastLevel: function(_tree) {
                var nodes = _tree.getSelectedNodes();
                for (var i = 0; i < nodes.length; i++) {
                    if (!nodes[i].isParent) {
                $.modal.msgError("不能选择最后层级节点（" + nodes[i].name + "）");
                        return false;
                    }
                }
                return true;
            },
            // 闅愯棌/鏄剧ず鎼滅储鏍?
            toggleSearch: function() {
                $('#search').slideToggle(200);
                $('#btnShow').toggle();
                $('#btnHide').toggle();
                $('#keyword').focus();
            },
            // 鎶樺彔
            collapse: function() {
                $._tree.expandAll(false);
            },
            // 灞曞紑
            expand: function() {
                $._tree.expandAll(true);
            }
        },
        // 閫氱敤鏂规硶灏佽澶勭悊
        common: {
            // 鍒ゆ柇瀛楃涓叉槸鍚︿负绌?
            isEmpty: function (value) {
                if (value == null || this.trim(value) == "" || value == undefined || value == "undefined") {
                    return true;
                }
                return false;
            },
            // 鍒ゆ柇涓€涓瓧绗︿覆鏄惁涓洪潪绌轰覆
            isNotEmpty: function (value) {
                return !$.common.isEmpty(value);
            },
            // 绌哄璞¤浆瀛楃涓?
            nullToStr: function(value) {
                if ($.common.isEmpty(value)) {
                    return "-";
                }
                return value;
            },
            // 鏄惁鏄剧ず鏁版嵁 涓虹┖榛樿涓烘樉绀?
            visible: function (value) {
                if ($.common.isEmpty(value) || value == true) {
                    return true;
                }
                return false;
            },
            // 绌烘牸鎴彇
            trim: function (value) {
                if (value == null) {
                    return "";
                }
                return value.toString().replace(/(^\s*)|(\s*$)|\r|\n/g, "");
            },
            // 姣旇緝涓や釜瀛楃涓诧紙澶у皬鍐欐晱鎰燂級
            equals: function (str, that) {
                return str == that;
            },
            // 姣旇緝涓や釜瀛楃涓诧紙澶у皬鍐欎笉鏁忔劅锛?
            equalsIgnoreCase: function (str, that) {
                return String(str).toUpperCase() === String(that).toUpperCase();
            },
            // 灏嗗瓧绗︿覆鎸夋寚瀹氬瓧绗﹀垎鍓?
            split: function (str, sep, maxLen) {
                if ($.common.isEmpty(str)) {
                    return null;
                }
                var value = String(str).split(sep);
                return maxLen ? value.slice(0, maxLen - 1) : value;
            },
            // 瀛楃涓叉牸寮忓寲(%s )
            sprintf: function (str) {
                var args = arguments, flag = true, i = 1;
                str = str.replace(/%s/g, function () {
                    var arg = args[i++];
                    if (typeof arg === 'undefined') {
                        flag = false;
                        return '';
                    }
                    return arg == null ? '' : arg;
                });
                return flag ? str : '';
            },
            // 鏃ユ湡鏍煎紡鍖?鏃堕棿鎴? -> yyyy-MM-dd HH-mm-ss
            dateFormat: function(date, format) {
                var that = this;
                if (that.isEmpty(date)) return "";
                if (!date) return;
                if (!format) format = "yyyy-MM-dd";
                switch (typeof date) {
                case "string":
                    date = new Date(date.replace(/-/g, "/"));
                    break;
                case "number":
                    date = new Date(date);
                    break;
                }
                if (!date instanceof Date) return;
                var dict = {
                    "yyyy": date.getFullYear(),
                    "M": date.getMonth() + 1,
                    "d": date.getDate(),
                    "H": date.getHours(),
                    "m": date.getMinutes(),
                    "s": date.getSeconds(),
                    "MM": ("" + (date.getMonth() + 101)).substr(1),
                    "dd": ("" + (date.getDate() + 100)).substr(1),
                    "HH": ("" + (date.getHours() + 100)).substr(1),
                    "mm": ("" + (date.getMinutes() + 100)).substr(1),
                    "ss": ("" + (date.getSeconds() + 100)).substr(1)
                };
                return format.replace(/(yyyy|MM?|dd?|HH?|ss?|mm?)/g,
                function() {
                    return dict[arguments[0]];
                });
            },
            // 鑾峰彇鑺傜偣鏁版嵁锛屾敮鎸佸灞傜骇璁块棶
            getItemField: function (item, field) {
                var value = item;
                if (typeof field !== 'string' || item.hasOwnProperty(field)) {
                    return item[field];
                }
                var props = field.split('.');
                for (var p in props) {
                    value = value && value[props[p]];
                }
                return value;
            },
            // 鎸囧畾闅忔満鏁拌繑鍥?
            random: function (min, max) {
                return Math.floor((Math.random() * max) + min);
            },
            // 鍒ゆ柇瀛楃涓叉槸鍚︽槸浠tart寮€澶?
            startWith: function(value, start) {
                var reg = new RegExp("^" + start);
                return reg.test(value)
            },
            // 鍒ゆ柇瀛楃涓叉槸鍚︽槸浠nd缁撳熬
            endWith: function(value, end) {
                var reg = new RegExp(end + "$");
                return reg.test(value)
            },
            // 鏁扮粍鍘婚噸
            uniqueFn: function(array) {
                var result = [];
                var hashObj = {};
                for (var i = 0; i < array.length; i++) {
                    if (!hashObj[array[i]]) {
                        hashObj[array[i]] = true;
                        result.push(array[i]);
                    }
                }
                return result;
            },
            // 鏁扮粍涓殑鎵€鏈夊厓绱犳斁鍏ヤ竴涓瓧绗︿覆
            join: function(array, separator) {
                if ($.common.isEmpty(array)) {
                    return null;
                }
                return array.join(separator);
            },
            // 鑾峰彇form涓嬫墍鏈夌殑瀛楁骞惰浆鎹负json瀵硅薄
            formToJSON: function(formId) {
                var json = {};
                $.each($("#" + formId).serializeArray(), function(i, field) {
                    if(json[field.name]) {
                        json[field.name] += ("," + field.value);
                    } else {
                        json[field.name] = field.value;
                    }
                });
                return json;
            },
            // 鏁版嵁瀛楀吀杞笅鎷夋
            dictToSelect: function(datas, value, name) {
                var actions = [];
                actions.push($.common.sprintf("<select class='form-control' name='%s'>", name));
                $.each(datas, function(index, dict) {
                    actions.push($.common.sprintf("<option value='%s'", dict.dictValue));
                    if (dict.dictValue == ('' + value)) {
                        actions.push(' selected');
                    }
                    actions.push($.common.sprintf(">%s</option>", dict.dictLabel));
                });
                actions.push('</select>');
                return actions.join('');
            },
            // 鑾峰彇obj瀵硅薄闀垮害
            getLength: function(obj) {
                var count = 0;
                for (var i in obj) {
                    if (obj.hasOwnProperty(i)) {
                        count++;
                    }
                }
                return count;
            },
            // 鍒ゆ柇绉诲姩绔?
            isMobile: function () {
                return navigator.userAgent.match(/(Android|iPhone|SymbianOS|Windows Phone|iPad|iPod)/i);
            },
            // 鏁板瓧姝ｅ垯琛ㄨ揪寮忥紝鍙兘涓?-9鏁板瓧
            numValid : function(text){
                var patten = new RegExp(/^[0-9]+$/);
                return patten.test(text);
            },
            // 鑻辨枃姝ｅ垯琛ㄨ揪寮忥紝鍙兘涓篴-z鍜孉-Z瀛楁瘝
            enValid : function(text){
                var patten = new RegExp(/^[a-zA-Z]+$/);
                return patten.test(text);
            },
            // 鑻辨枃銆佹暟瀛楁鍒欒〃杈惧紡锛屽繀椤诲寘鍚紙瀛楁瘝锛屾暟瀛楋級
            enNumValid : function(text){
                var patten = new RegExp(/^(?=.*[a-zA-Z]+)(?=.*[0-9]+)[a-zA-Z0-9]+$/);
                return patten.test(text);
            },
            // 鑻辨枃銆佹暟瀛椼€佺壒娈婂瓧绗︽鍒欒〃杈惧紡锛屽繀椤诲寘鍚紙瀛楁瘝锛屾暟瀛楋紝鐗规畩瀛楃!@#$%^&*()-=_+锛?
            charValid : function(text){
                var patten = new RegExp(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[~!@#\$%\^&\*\(\)\-=_\+])[A-Za-z\d~!@#\$%\^&\*\(\)\-=_\+]{6,}$/);
                return patten.test(text);
            },
        }
    });
})(jQuery);

/** 琛ㄦ牸绫诲瀷 */
table_type = {
    bootstrapTable: 0,
    bootstrapTreeTable: 1
};

/** 娑堟伅鐘舵€佺爜 */
web_status = {
    SUCCESS: 0,
    FAIL: 500,
    WARNING: 301
};

/** 寮圭獥鐘舵€佺爜 */
modal_status = {
    SUCCESS: "success",
    FAIL: "error",
    WARNING: "warning"
};
