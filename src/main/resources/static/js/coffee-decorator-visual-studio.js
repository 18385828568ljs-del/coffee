(function (window, $) {
    'use strict';

    var PRESETS = {
        LEFT_COPY_RIGHT_SUBJECT:{ label:'左文右图', subject:{x:.60,y:.15,width:.32,height:.70}, safe:{x:.06,y:.18,width:.44,height:.56} },
        RIGHT_COPY_LEFT_SUBJECT:{ label:'右文左图', subject:{x:.08,y:.15,width:.32,height:.70}, safe:{x:.50,y:.18,width:.44,height:.56} },
        CENTER_SUBJECT:{ label:'居中主体', subject:{x:.36,y:.15,width:.28,height:.68}, safe:{x:.05,y:.18,width:.25,height:.54} },
        TOP_COPY_BOTTOM_SUBJECT:{ label:'上文下图', subject:{x:.34,y:.43,width:.32,height:.50}, safe:{x:.10,y:.07,width:.80,height:.25} },
        SCENE_MOOD:{ label:'场景氛围', subject:{x:.65,y:.30,width:.24,height:.54}, safe:{x:.07,y:.18,width:.40,height:.54} }
    };
    var TARGET_LABELS = { canvas:'整个画面', subject_1:'主体', safe_1:'文字安全区 1', safe_2:'文字安全区 2' };
    var options = {};
    var context = {};
    var studio = null;
    var state = null;
    var history = [];
    var historyIndex = -1;
    var drawing = null;
    var interactionStart = null;
    var suppressStageClick = false;

    function defaultState() {
        var preset = PRESETS.LEFT_COPY_RIGHT_SUBJECT;
        return {
            schemaVersion:1,
            generationMode:'SKETCH',
            compositionPreset:'LEFT_COPY_RIGHT_SUBJECT',
            subject:{ id:'subject_1', visible:true, type:'COFFEE_CUP', box:copy(preset.subject) },
            safeAreas:[{ id:'safe_1', box:copy(preset.safe) }],
            sceneType:'CAFE',
            renderingStyle:'COMMERCIAL_PHOTO',
            mood:'WARM',
            lighting:'SOFT_NATURAL',
            decorDensity:'LOW',
            primaryColor:'#8b5e3c',
            notes:[],
            marks:[],
            selectedTarget:'subject_1',
            tool:'select'
        };
    }

    function copy(value) { return JSON.parse(JSON.stringify(value)); }
    function clamp(value, min, max) { return Math.max(min, Math.min(max, value)); }
    function round(value) { return Math.round(value * 1000) / 1000; }
    function canvasSpec() {
        return {
            width:Number(context.logicalWidth || 750),
            height:Number(context.logicalHeight || 360),
            renderMode:String(context.renderMode || 'COVER')
        };
    }

    function mount() {
        if (studio) return;
        var html = [
            '<section id="visualStudio" class="vs-shell" hidden aria-label="AI 视觉工作室">',
            '  <header class="vs-topbar">',
            '    <button type="button" class="vs-icon-btn vs-close" title="返回装修工作台" aria-label="返回装修工作台"><i class="fa fa-arrow-left"></i></button>',
            '    <div class="vs-heading"><strong>AI 视觉工作室</strong><span id="vsComponentMeta">首页 · 顶部轮播图</span></div>',
            '    <div class="vs-topbar-tools">',
            '      <button type="button" class="vs-icon-btn vs-undo" title="撤销" aria-label="撤销"><i class="fa fa-undo"></i></button>',
            '      <button type="button" class="vs-icon-btn vs-redo" title="重做" aria-label="重做"><i class="fa fa-repeat"></i></button>',
            '      <button type="button" class="vs-icon-btn vs-close" title="关闭" aria-label="关闭"><i class="fa fa-times"></i></button>',
            '    </div>',
            '  </header>',
            '  <div class="vs-workspace">',
            '    <aside class="vs-left">',
            '      <section class="vs-section"><h2 class="vs-section-head">构图模板</h2><div class="vs-template-list"></div></section>',
            '      <section class="vs-section"><h2 class="vs-section-head">画面区域</h2><div class="vs-element-tools">',
            '        <button type="button" class="vs-icon-btn vs-toggle-subject" title="显示或隐藏主体" aria-label="显示或隐藏主体"><i class="fa fa-coffee"></i></button>',
            '        <button type="button" class="vs-icon-btn vs-add-safe" title="增加文字安全区" aria-label="增加文字安全区"><i class="fa fa-font"></i></button>',
            '        <button type="button" class="vs-icon-btn vs-remove-element" title="删除选中区域" aria-label="删除选中区域"><i class="fa fa-trash"></i></button>',
            '        <button type="button" class="vs-icon-btn vs-select-tool" title="选择和移动" aria-label="选择和移动"><i class="fa fa-mouse-pointer"></i></button>',
            '      </div></section>',
            '      <section class="vs-section"><h2 class="vs-section-head">辅助标注</h2><div class="vs-mark-tools">',
            '        <button type="button" class="vs-icon-btn vs-mark-tool" data-tool="pen" title="画笔" aria-label="画笔"><i class="fa fa-pencil"></i></button>',
            '        <button type="button" class="vs-icon-btn vs-mark-tool" data-tool="arrow" title="箭头" aria-label="箭头"><i class="fa fa-long-arrow-right"></i></button>',
            '        <button type="button" class="vs-icon-btn vs-mark-tool" data-tool="circle" title="圈选" aria-label="圈选"><i class="fa fa-circle-o"></i></button>',
            '        <button type="button" class="vs-icon-btn vs-clear-marks" title="清除辅助标注" aria-label="清除辅助标注"><i class="fa fa-eraser"></i></button>',
            '      </div></section>',
            '    </aside>',
            '    <main class="vs-canvas-column">',
            '      <div class="vs-canvas-head"><strong>模块画面草图</strong><span>750 × 360 rpx · 业务文字独立渲染</span></div>',
            '      <div class="vs-stage-wrap"><div class="vs-stage" id="vsStage"><canvas class="vs-sketch-layer" width="750" height="360"></canvas></div></div>',
            '      <section class="vs-results" hidden><div class="vs-results-head"><strong>AI 候选</strong><span class="vs-results-meta"></span></div><div class="vs-results-grid"></div>',
            '        <div class="vs-refine"><button type="button" data-refine="subject-bigger">主体更大</button><button type="button" data-refine="safe-clean">文字区更干净</button><button type="button" data-refine="brighter">画面更亮</button><button type="button" data-refine="decor-less">装饰更少</button></div>',
            '      </section>',
            '    </main>',
            '    <aside class="vs-right">',
            '      <section class="vs-section"><h2 class="vs-section-head">生成方式</h2><div class="vs-generation-modes">',
            '        <button type="button" data-mode="SKETCH">草图设计</button><button type="button" data-mode="RESTYLE">参考现有背景改图</button>',
            '      </div></section>',
            '      <section class="vs-section"><h2 class="vs-section-head">画面选择</h2>',
            '        <div class="vs-field"><label for="vsSubjectType">主要主体</label><select id="vsSubjectType"><option value="COFFEE_CUP">咖啡杯</option><option value="DESSERT">甜点</option><option value="COFFEE_BEANS">咖啡豆</option><option value="STORE">门店环境</option></select></div>',
            '        <div class="vs-field"><label for="vsSceneType">场景</label><select id="vsSceneType"><option value="CAFE">咖啡馆</option><option value="WINDOW">窗边自然光</option><option value="STUDIO">纯色摄影棚</option><option value="ABSTRACT">抽象材质</option></select></div>',
            '        <div class="vs-field"><label for="vsRenderingStyle">画面风格</label><select id="vsRenderingStyle"><option value="COMMERCIAL_PHOTO">商业摄影</option><option value="HAND_DRAWN">手绘插画</option><option value="COLLAGE">纸张拼贴</option><option value="MINIMAL_3D">简洁 3D</option></select></div>',
            '        <div class="vs-field"><label for="vsPrimaryColor">主色调</label><div class="vs-color-line"><input id="vsPrimaryColor" type="color"><input id="vsPrimaryColorText" type="text" maxlength="7"></div></div>',
            '        <div class="vs-field"><label for="vsMood">氛围</label><select id="vsMood"><option value="WARM">温暖松弛</option><option value="FRESH">清新明亮</option><option value="PREMIUM">克制高级</option><option value="PLAYFUL">活泼亲切</option></select></div>',
            '        <div class="vs-field"><label for="vsLighting">光线</label><select id="vsLighting"><option value="SOFT_NATURAL">柔和自然光</option><option value="BRIGHT">明亮通透</option><option value="DRAMATIC">明暗对比</option><option value="FLAT">均匀柔光</option></select></div>',
            '        <div class="vs-field"><label for="vsDecorDensity">装饰密度</label><select id="vsDecorDensity"><option value="NONE">无装饰</option><option value="LOW">少量</option><option value="MEDIUM">适中</option></select></div>',
            '      </section>',
            '      <section class="vs-section vs-sketch-only"><h2 class="vs-section-head">区域备注</h2><div class="vs-note-target"></div>',
            '        <div class="vs-note-row"><select id="vsNoteAction"><option value="KEEP">保持</option><option value="INCREASE">增加</option><option value="REDUCE">减少</option><option value="MOVE">移动</option><option value="AVOID">避开</option><option value="REPLACE">更换</option></select><textarea id="vsNoteText" maxlength="200" placeholder="填写这一处的要求"></textarea></div>',
            '        <button type="button" class="btn btn-default btn-sm vs-note-add">添加备注</button><div class="vs-note-list"></div>',
            '      </section>',
            '      <section class="vs-section vs-sketch-only"><h2 class="vs-section-head">AI 理解结果</h2><ul class="vs-summary"></ul></section>',
            '    </aside>',
            '  </div>',
            '  <footer class="vs-footer"><div class="vs-status" role="status"></div><div class="vs-footer-actions">',
            '    <button type="button" class="btn btn-default vs-reset"><i class="fa fa-refresh"></i> 重置草图</button>',
            '    <button type="button" class="btn btn-default vs-save"><i class="fa fa-save"></i> 保存意图草稿</button>',
            '    <button type="button" class="btn btn-primary vs-generate"><i class="fa fa-magic"></i> 检查并生成 2 个方案</button>',
            '  </div></footer>',
            '</section>'
        ].join('');
        $('body').append(html);
        studio = $('#visualStudio');
        bindEvents();
        renderTemplates();
    }

    function bindEvents() {
        studio.on('click','.vs-close',close);
        studio.on('click','.vs-template',function () { applyPreset($(this).data('preset')); });
        studio.on('click','.vs-generation-modes button',function () {
            if (this.disabled) return;
            state.generationMode=$(this).data('mode')==='RESTYLE' ? 'RESTYLE' : 'SKETCH';
            commit(); renderGenerationMode();
            setStatus(state.generationMode==='RESTYLE' ? '将保留当前背景构图，只调整风格、色彩、材质和光影。' : '选择构图并补充画面信息。');
        });
        studio.on('click','.vs-toggle-subject',function () { state.subject.visible=!state.subject.visible; commit(); render(); });
        studio.on('click','.vs-add-safe',addSafeArea);
        studio.on('click','.vs-remove-element',removeSelected);
        studio.on('click','.vs-select-tool',function () { setTool('select'); });
        studio.on('click','.vs-mark-tool',function () { setTool($(this).data('tool')); });
        studio.on('click','.vs-clear-marks',function () { state.marks=[]; state.selectedTarget='canvas'; commit(); render(); });
        studio.on('click','.vs-semantic-box',function (event) { event.stopPropagation(); state.selectedTarget=$(this).data('target'); renderSelection(); renderNotes(); });
        studio.on('click','.vs-stage',function () {
            if (suppressStageClick) { suppressStageClick=false; return; }
            if (state.tool==='select') { state.selectedTarget='canvas'; renderSelection(); renderNotes(); }
        });
        studio.on('click','.vs-note-add',addNote);
        studio.on('click','.vs-note-item button',function () { state.notes.splice(Number($(this).data('index')),1); commit(); renderNotes(); renderSummary(); });
        studio.on('change','#vsSubjectType,#vsSceneType,#vsRenderingStyle,#vsMood,#vsLighting,#vsDecorDensity',readQuestions);
        studio.on('input','#vsPrimaryColor',function () { $('#vsPrimaryColorText').val(this.value); readQuestions(); });
        studio.on('change','#vsPrimaryColorText',function () { if (/^#[0-9a-f]{6}$/i.test(this.value)) { $('#vsPrimaryColor').val(this.value); readQuestions(); } });
        studio.on('click','.vs-reset',function () { state=defaultState(); resetHistory(); render(); setStatus('草图已恢复为推荐构图。'); });
        studio.on('click','.vs-save',saveDraft);
        studio.on('click','.vs-generate',generate);
        studio.on('click','.vs-undo',undo);
        studio.on('click','.vs-redo',redo);
        studio.on('click','.vs-result-actions button',handleResultAction);
        studio.on('click','[data-refine]',function () { refine($(this).data('refine')); });
        var canvas=studio.find('.vs-sketch-layer')[0];
        canvas.addEventListener('pointerdown',startMark);
        canvas.addEventListener('pointermove',moveMark);
        canvas.addEventListener('pointerup',finishMark);
        canvas.addEventListener('pointercancel',finishMark);
        $(window).on('resize.visualStudio',function () { if (isOpen()) layoutBoxes(); });
        $(document).on('keydown.visualStudio',function (event) {
            if (!isOpen()) return;
            if (event.key==='Escape') close();
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase()==='z') { event.preventDefault(); event.shiftKey ? redo() : undo(); }
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase()==='y') { event.preventDefault(); redo(); }
        });
    }

    function open(openContext) {
        mount();
        context=openContext || {};
        var saved=loadDraft();
        state=saved || defaultState();
        if (!state.subject || !state.safeAreas) state=defaultState();
        if (!state.generationMode) state.generationMode='SKETCH';
        if (state.generationMode==='RESTYLE' && !context.canRestyle) state.generationMode='SKETCH';
        resetHistory();
        studio.prop('hidden',false);
        $('body').addClass('vs-open');
        var canvas=canvasSpec(), maxWidth=Math.min(900,Math.max(320,Math.round(680*canvas.width/canvas.height)));
        $('#vsComponentMeta').text((context.componentName || '当前组件') + ' · ' + canvas.width + ' × ' + canvas.height + ' rpx');
        studio.find('.vs-canvas-head span').text(canvas.width + ' × ' + canvas.height + ' rpx · 业务文字独立渲染');
        studio.find('.vs-stage-wrap').css('max-width',maxWidth+'px');
        studio.find('.vs-stage').css('aspect-ratio',canvas.width+' / '+canvas.height);
        studio.find('.vs-sketch-layer').attr({width:canvas.width,height:canvas.height});
        studio.find('.vs-results').prop('hidden',true);
        setBusy(false);
        setStatus(saved ? '已恢复本机保存的意图草稿。' : '选择构图并补充画面信息。');
        render();
    }

    function close() {
        if (!studio) return;
        persist();
        studio.prop('hidden',true);
        $('body').removeClass('vs-open');
    }

    function isOpen() { return !!studio && !studio.prop('hidden'); }

    function storageKey() {
        return 'coffee.visualIntent.v1.' + String(context.merchantId || 'merchant') + '.' + String(context.themeId || 'theme') + '.' + String(context.slotKey || 'component');
    }

    function loadDraft() {
        try { var value=window.localStorage.getItem(storageKey()); return value ? JSON.parse(value) : null; }
        catch (error) { return null; }
    }

    function persist() {
        try { window.localStorage.setItem(storageKey(),JSON.stringify(state)); return true; }
        catch (error) { setStatus('本机草稿保存失败，请检查浏览器存储权限。',true); return false; }
    }

    function saveDraft() {
        if (persist()) setStatus('意图草稿已保存在本机。');
    }

    function renderTemplates() {
        var host=studio.find('.vs-template-list').empty();
        Object.keys(PRESETS).forEach(function (key) {
            var preset=PRESETS[key], preview=$('<div class="vs-template-preview"><i class="copy"></i><i class="subject"></i></div>');
            positionMini(preview.find('.copy'),preset.safe);
            positionMini(preview.find('.subject'),preset.subject);
            $('<button type="button" class="vs-template"><span></span></button>').attr('data-preset',key)
                .prepend(preview).find('span').text(preset.label).end().appendTo(host);
        });
    }

    function positionMini(element, box) {
        element.css({left:(box.x*100)+'%',top:(box.y*100)+'%',width:(box.width*100)+'%',height:(box.height*100)+'%'});
    }

    function render() {
        renderGenerationMode();
        renderControls();
        renderBoxes();
        redrawMarks();
        renderNotes();
        renderSummary();
        updateHistoryButtons();
    }

    function renderGenerationMode() {
        if (!studio || !state) return;
        studio.toggleClass('is-restyle',state.generationMode==='RESTYLE');
        studio.find('.vs-generation-modes button').removeClass('is-active')
            .filter('[data-mode="'+state.generationMode+'"]').addClass('is-active');
        studio.find('.vs-generation-modes [data-mode="RESTYLE"]').prop('disabled',!context.canRestyle)
            .attr('title',context.canRestyle ? '保留当前背景构图' : '当前组件还没有可供参考的背景图片');
    }

    function renderControls() {
        studio.find('.vs-template').removeClass('is-active').filter('[data-preset="'+state.compositionPreset+'"]').addClass('is-active');
        $('#vsSubjectType').val(state.subject.type);
        $('#vsSceneType').val(state.sceneType);
        $('#vsRenderingStyle').val(state.renderingStyle);
        $('#vsMood').val(state.mood);
        $('#vsLighting').val(state.lighting);
        $('#vsDecorDensity').val(state.decorDensity);
        $('#vsPrimaryColor,#vsPrimaryColorText').val(state.primaryColor);
        studio.find('.vs-toggle-subject').toggleClass('is-active',state.subject.visible);
        studio.find('.vs-mark-tool,.vs-select-tool').removeClass('is-active');
        studio.find(state.tool==='select' ? '.vs-select-tool' : '.vs-mark-tool[data-tool="'+state.tool+'"]').addClass('is-active');
        studio.find('.vs-stage').toggleClass('is-drawing',state.tool!=='select');
    }

    function renderBoxes() {
        var stage=studio.find('.vs-stage');
        stage.find('.vs-semantic-box').remove();
        if (state.subject.visible) addBox(stage,state.subject.id,'主体 · '+subjectLabel(state.subject.type),state.subject.box,'is-subject');
        state.safeAreas.forEach(function (area,index) { addBox(stage,area.id,'文字安全区 '+(index+1),area.box,'is-safe'); });
        renderSelection();
    }

    function addBox(stage, id, label, box, className) {
        var element=$('<div class="vs-semantic-box"><span></span><button type="button" class="vs-resize-handle" title="缩放区域" aria-label="缩放区域"></button></div>')
            .addClass(className).attr('data-target',id).data('target',id).find('span').text(label).end().appendTo(stage);
        setBoxCss(element,box);
        element.draggable({containment:'parent',cancel:'.vs-resize-handle',start:function(){interactionStart=serialize();},stop:function(){syncBox(element,id); finishInteraction();}});
        bindResizeHandle(element,id);
    }

    function bindResizeHandle(element,id) {
        element.find('.vs-resize-handle').on('pointerdown',function (event) {
            event.preventDefault(); event.stopPropagation();
            interactionStart=serialize();
            var startX=event.clientX, startY=event.clientY, startWidth=element.outerWidth(), startHeight=element.outerHeight();
            var stage=studio.find('.vs-stage'), maxWidth=stage.width()-element.position().left, maxHeight=stage.height()-element.position().top;
            function resize(moveEvent) {
                element.css({width:clamp(startWidth+moveEvent.clientX-startX,60,maxWidth),height:clamp(startHeight+moveEvent.clientY-startY,38,maxHeight)});
            }
            function stopResize() {
                $(document).off('.visualStudioResize');
                syncBox(element,id); finishInteraction();
            }
            $(document).on('pointermove.visualStudioResize',resize).one('pointerup.visualStudioResize pointercancel.visualStudioResize',stopResize);
        });
    }

    function setBoxCss(element, box) {
        element.css({left:(box.x*100)+'%',top:(box.y*100)+'%',width:(box.width*100)+'%',height:(box.height*100)+'%'});
    }

    function layoutBoxes() {
        studio.find('.vs-semantic-box').each(function () {
            var id=$(this).data('target'), target=findBox(id);
            if (target) setBoxCss($(this),target);
        });
    }

    function findBox(id) {
        if (id==='subject_1') return state.subject.box;
        var area=state.safeAreas.filter(function (item) { return item.id===id; })[0];
        return area && area.box;
    }

    function syncBox(element,id) {
        var stage=studio.find('.vs-stage'), box=findBox(id);
        if (!box) return;
        box.x=round(element.position().left/stage.width());
        box.y=round(element.position().top/stage.height());
        box.width=round(Math.min(element.outerWidth()/stage.width(),1-box.x));
        box.height=round(Math.min(element.outerHeight()/stage.height(),1-box.y));
        setBoxCss(element,box);
        state.selectedTarget=id;
    }

    function finishInteraction() {
        if (interactionStart!==serialize()) commit();
        interactionStart=null;
        renderSelection(); renderSummary(); renderNotes();
    }

    function renderSelection() {
        studio.find('.vs-semantic-box').removeClass('is-selected').filter('[data-target="'+state.selectedTarget+'"]').addClass('is-selected');
        studio.find('.vs-remove-element').prop('disabled',state.selectedTarget==='canvas');
    }

    function applyPreset(key) {
        var preset=PRESETS[key];
        if (!preset) return;
        state.compositionPreset=key;
        state.subject.visible=true;
        state.subject.box=copy(preset.subject);
        state.safeAreas=[{id:'safe_1',box:copy(preset.safe)}];
        state.selectedTarget='subject_1';
        commit(); render();
    }

    function addSafeArea() {
        if (state.safeAreas.length>=2) return setStatus('文字安全区最多两个。',true);
        var id='safe_2';
        state.safeAreas.push({id:id,box:{x:.06,y:.70,width:.42,height:.20}});
        state.selectedTarget=id;
        commit(); render();
    }

    function removeSelected() {
        if (state.selectedTarget==='subject_1') state.subject.visible=false;
        else if (state.selectedTarget.indexOf('mark_')===0) state.marks=state.marks.filter(function (item) { return item.id!==state.selectedTarget; });
        else state.safeAreas=state.safeAreas.filter(function (item) { return item.id!==state.selectedTarget; });
        state.notes=state.notes.filter(function (item) { return item.targetId!==state.selectedTarget; });
        state.selectedTarget='canvas';
        commit(); render();
    }

    function setTool(tool) {
        state.tool=tool;
        renderControls();
    }

    function pointFromEvent(event) {
        var rect=studio.find('.vs-sketch-layer')[0].getBoundingClientRect();
        return {x:clamp((event.clientX-rect.left)/rect.width,0,1),y:clamp((event.clientY-rect.top)/rect.height,0,1)};
    }

    function startMark(event) {
        if (!state || state.tool==='select') return;
        if (state.marks.length>=6) { setStatus('辅助标注最多六个。',true); setTool('select'); return; }
        event.currentTarget.setPointerCapture(event.pointerId);
        var point=pointFromEvent(event);
        drawing={id:'mark_'+Date.now(),tool:state.tool,points:[point,point]};
        state.marks.push(drawing);
        redrawMarks();
    }

    function moveMark(event) {
        if (!drawing) return;
        var point=pointFromEvent(event);
        if (drawing.tool==='pen') {
            var last=drawing.points[drawing.points.length-1], distance=Math.abs(point.x-last.x)+Math.abs(point.y-last.y);
            if (distance>.008 && drawing.points.length<80) drawing.points.push(point);
        } else drawing.points[1]=point;
        redrawMarks();
    }

    function finishMark() {
        if (!drawing) return;
        state.selectedTarget=drawing.id;
        TARGET_LABELS[drawing.id]='标注区域 '+state.marks.length;
        drawing=null;
        suppressStageClick=true;
        state.tool='select';
        commit(); render();
    }

    function redrawMarks(targetCanvas,preserveCanvas) {
        var canvas=targetCanvas || studio.find('.vs-sketch-layer')[0];
        if (!canvas || !state) return;
        var ctx=canvas.getContext('2d'), width=canvas.width, height=canvas.height;
        if (!preserveCanvas) ctx.clearRect(0,0,width,height);
        ctx.strokeStyle='#a13e38'; ctx.lineWidth=4; ctx.lineCap='round'; ctx.lineJoin='round';
        state.marks.forEach(function (mark) {
            var points=mark.points || [];
            if (points.length<2) return;
            var start=points[0], end=points[points.length-1];
            ctx.beginPath();
            if (mark.tool==='pen') {
                ctx.moveTo(start.x*width,start.y*height);
                points.slice(1).forEach(function (point) { ctx.lineTo(point.x*width,point.y*height); });
            } else if (mark.tool==='circle') {
                ctx.ellipse((start.x+end.x)*width/2,(start.y+end.y)*height/2,Math.abs(end.x-start.x)*width/2,Math.abs(end.y-start.y)*height/2,0,0,Math.PI*2);
            } else {
                drawArrow(ctx,start.x*width,start.y*height,end.x*width,end.y*height);
            }
            ctx.stroke();
        });
    }

    function drawArrow(ctx,x1,y1,x2,y2) {
        var angle=Math.atan2(y2-y1,x2-x1), head=14;
        ctx.moveTo(x1,y1); ctx.lineTo(x2,y2);
        ctx.moveTo(x2,y2); ctx.lineTo(x2-head*Math.cos(angle-Math.PI/6),y2-head*Math.sin(angle-Math.PI/6));
        ctx.moveTo(x2,y2); ctx.lineTo(x2-head*Math.cos(angle+Math.PI/6),y2-head*Math.sin(angle+Math.PI/6));
    }

    function readQuestions() {
        state.subject.type=$('#vsSubjectType').val();
        state.sceneType=$('#vsSceneType').val();
        state.renderingStyle=$('#vsRenderingStyle').val();
        state.mood=$('#vsMood').val();
        state.lighting=$('#vsLighting').val();
        state.decorDensity=$('#vsDecorDensity').val();
        state.primaryColor=$('#vsPrimaryColor').val();
        commit(); renderSummary(); renderBoxes();
    }

    function addNote() {
        var text=$('#vsNoteText').val().trim();
        if (!text) return setStatus('请先填写区域备注。',true);
        if (state.notes.length>=10) return setStatus('区域备注最多十条。',true);
        state.notes.push({targetId:state.selectedTarget,action:$('#vsNoteAction').val(),text:text});
        $('#vsNoteText').val('');
        commit(); renderNotes(); renderSummary();
    }

    function renderNotes() {
        var label=targetLabel(state.selectedTarget);
        studio.find('.vs-note-target').text('当前锚点：'+label);
        var host=studio.find('.vs-note-list').empty();
        state.notes.forEach(function (note,index) {
            var item=$('<div class="vs-note-item"><span></span><button type="button" title="删除备注" aria-label="删除备注"><i class="fa fa-times"></i></button></div>');
            item.find('span').text(actionLabel(note.action)+' · '+targetLabel(note.targetId)+' · '+note.text);
            item.find('button').attr('data-index',index).data('index',index);
            host.append(item);
        });
    }

    function renderSummary() {
        var safe=state.safeAreas.map(function (area) { return positionLabel(area.box); }).join('、');
        var lines=[
            '用途：'+(context.componentName || '当前组件')+'背景',
            '主体：'+(state.subject.visible ? subjectLabel(state.subject.type)+'，'+positionLabel(state.subject.box) : '不设置独立主体'),
            '文字安全区：'+(safe || '尚未设置'),
            '场景：'+selectText('#vsSceneType',state.sceneType)+'，'+selectText('#vsLighting',state.lighting),
            '风格：'+selectText('#vsRenderingStyle',state.renderingStyle)+'，'+selectText('#vsMood',state.mood),
            '主色：'+state.primaryColor+'；装饰：'+selectText('#vsDecorDensity',state.decorDensity),
            '禁止出现：业务文字、价格、按钮、二维码'
        ];
        var host=studio.find('.vs-summary').empty();
        lines.forEach(function (line) { $('<li>').text(line).appendTo(host); });
    }

    function selectText(selector,value) {
        var option=studio.find(selector+' option[value="'+value+'"]');
        return option.length ? option.text() : value;
    }

    function subjectLabel(value) {
        return {COFFEE_CUP:'咖啡杯',DESSERT:'甜点',COFFEE_BEANS:'咖啡豆',STORE:'门店环境'}[value] || '主体';
    }

    function targetLabel(id) { return TARGET_LABELS[id] || (id.indexOf('mark_')===0 ? '标注区域' : id); }
    function actionLabel(value) { return {KEEP:'保持',INCREASE:'增加',REDUCE:'减少',MOVE:'移动',AVOID:'避开',REPLACE:'更换'}[value] || value; }
    function positionLabel(box) {
        var center=box.x+box.width/2, vertical=box.y+box.height/2;
        return (center<.4?'左侧':center>.6?'右侧':'中间')+(vertical<.38?'上方':vertical>.65?'下方':'');
    }

    function overlapTooMuch(a,b) {
        var width=Math.max(0,Math.min(a.x+a.width,b.x+b.width)-Math.max(a.x,b.x));
        var height=Math.max(0,Math.min(a.y+a.height,b.y+b.height)-Math.max(a.y,b.y));
        return width*height/Math.min(a.width*a.height,b.width*b.height)>.25;
    }

    function validate() {
        if (!state.subject.visible && !state.sceneType) return '请设置一个主体或场景。';
        if (!state.safeAreas.length) return '至少需要一个文字安全区。';
        if (state.subject.visible && state.safeAreas.some(function (area) { return overlapTooMuch(state.subject.box,area.box); })) return '主体与文字安全区重叠过多，请先调整位置。';
        var conflicts={};
        for (var i=0;i<state.notes.length;i++) {
            var note=state.notes[i], key=note.targetId;
            if ((note.action==='INCREASE' && conflicts[key]==='REDUCE') || (note.action==='REDUCE' && conflicts[key]==='INCREASE')) return '同一区域存在“增加”和“减少”的冲突备注。';
            if (note.action==='INCREASE' || note.action==='REDUCE') conflicts[key]=note.action;
        }
        return '';
    }

    function visualIntent() {
        var canvas=canvasSpec();
        return {
            schemaVersion:1,
            slotKey:String(context.slotKey || ''),
            slotSpecVersion:Number(context.slotSpecVersion || 1),
            canvas:{width:canvas.width,height:canvas.height,renderMode:canvas.renderMode},
            visualPreset:state.mood,
            compositionPreset:state.compositionPreset,
            subjects:state.subject.visible ? [{id:'subject_1',type:state.subject.type,box:copy(state.subject.box),identityPolicy:'STYLE_GUIDE'}] : [],
            textSafeAreas:state.safeAreas.map(function (area) { return copy(area.box); }),
            scene:{type:state.sceneType,lighting:state.lighting,colorTone:state.primaryColor},
            elements:state.decorDensity==='NONE' ? [] : [{id:'decor_1',type:'COFFEE_DECOR',density:state.decorDensity,position:'EDGE'}],
            annotations:state.notes.map(function (note) { return copy(note); }),
            marks:copy(state.marks),
            questionAnswers:{renderingStyle:state.renderingStyle,mood:state.mood,decorDensity:state.decorDensity}
        };
    }

    function guideImage() {
        var spec=canvasSpec(), canvas=document.createElement('canvas'); canvas.width=spec.width; canvas.height=spec.height;
        var ctx=canvas.getContext('2d');
        ctx.fillStyle=state.primaryColor; ctx.fillRect(0,0,spec.width,spec.height);
        if (state.subject.visible) {
            var box=state.subject.box;
            ctx.fillStyle='rgba(255,190,120,.88)';
            ctx.fillRect(box.x*spec.width,box.y*spec.height,box.width*spec.width,box.height*spec.height);
        }
        state.safeAreas.forEach(function (area) {
            var box=area.box;
            ctx.fillStyle='rgba(220,250,232,.86)'; ctx.fillRect(box.x*spec.width,box.y*spec.height,box.width*spec.width,box.height*spec.height);
            ctx.strokeStyle='rgba(36,84,59,.9)'; ctx.lineWidth=3; ctx.setLineDash([12,8]); ctx.strokeRect(box.x*spec.width,box.y*spec.height,box.width*spec.width,box.height*spec.height); ctx.setLineDash([]);
        });
        redrawMarks(canvas,true);
        return canvas.toDataURL('image/png');
    }

    function promptText() {
        return subjectLabel(state.subject.type)+'；'+selectText('#vsSceneType',state.sceneType)+'；'+selectText('#vsRenderingStyle',state.renderingStyle)+'；'+selectText('#vsMood',state.mood)+'；文字安全区保持干净，所有业务文字由页面独立渲染。';
    }

    function generate() {
        var restyle=state.generationMode==='RESTYLE', error=restyle ? '' : validate();
        if (error) return setStatus(error,true);
        persist();
        setBusy(true); setStatus('正在创建 AI 生图任务...');
        var request={
            slotKey:String(context.slotKey || ''), generationType:'BACKGROUND', textMode:'NO_TEXT', candidateCount:2,
            stylePreset:selectText('#vsRenderingStyle',state.renderingStyle), primaryColor:state.primaryColor,
            prompt:promptText()
        };
        if (restyle) {
            request.referenceMode='INITIAL_SKIN';
            if (context.referenceAssetId) request.referenceAssetId=Number(context.referenceAssetId);
        } else {
            request.visualIntent=visualIntent(); request.guideImageDataUrl=guideImage();
        }
        if (options.onGenerate) options.onGenerate(request);
    }

    function setStatus(message,isError) {
        if (!studio) return;
        studio.find('.vs-status').text(message || '').toggleClass('is-error',!!isError);
    }

    function setBusy(busy) {
        if (!studio) return;
        studio.find('.vs-generate').prop('disabled',!!busy).html(busy ? '<i class="fa fa-spinner fa-spin"></i> 正在生成' : '<i class="fa fa-magic"></i> 检查并生成 2 个方案');
    }

    function setResults(results) {
        if (!studio || !isOpen()) return;
        results=results || [];
        var host=studio.find('.vs-results-grid').empty();
        results.forEach(function (result) {
            var item=$('<article class="vs-result"><img alt="AI 候选背景"><div class="vs-result-copy"><strong></strong><small></small><div class="vs-result-actions"><button type="button" class="btn btn-default btn-xs" data-action="preview">放到页面看看</button><button type="button" class="btn btn-default btn-xs" data-action="accept">保存到素材库</button><button type="button" class="btn btn-primary btn-xs" data-action="apply">应用到当前模块</button></div></div></article>');
            item.attr('data-result-id',result.id).data('result',result);
            item.find('img').attr('src',result.url).css('aspect-ratio',canvasSpec().width+' / '+canvasSpec().height); item.find('strong').text('方案 '+result.candidateNo);
            item.find('small').text((result.finalWidth || result.width)+' × '+(result.finalHeight || result.height)+' px');
            host.append(item);
        });
        studio.find('.vs-results').prop('hidden',!results.length);
        studio.find('.vs-results-meta').text(results.length+' 个可用方案');
        setBusy(false); setStatus(results.length ? '生成完成。候选图尚未写入装修草稿。' : '任务完成，但没有返回候选图。',!results.length);
    }

    function handleResultAction() {
        var item=$(this).closest('.vs-result'), result=item.data('result'), action=$(this).data('action');
        if (!result) return;
        if (action==='preview' && options.onPreview) options.onPreview(result);
        if (action==='accept' && options.onAccept) options.onAccept(result.id);
        if (action==='apply' && options.onApply) options.onApply(result.id);
    }

    function refine(action) {
        if (action==='subject-bigger' && state.subject.visible) {
            var box=state.subject.box, nextWidth=Math.min(.55,box.width+.08), nextHeight=Math.min(.82,box.height+.08);
            box.x=round(clamp(box.x-(nextWidth-box.width)/2,0,1-nextWidth)); box.y=round(clamp(box.y-(nextHeight-box.height)/2,0,1-nextHeight)); box.width=nextWidth; box.height=nextHeight;
        }
        if (action==='safe-clean') state.notes.push({targetId:state.safeAreas[0].id,action:'KEEP',text:'保持低纹理、高对比、无主体和装饰'});
        if (action==='brighter') state.lighting='BRIGHT';
        if (action==='decor-less') state.decorDensity='LOW';
        commit(); render(); generate();
    }

    function serialize() { return JSON.stringify(state); }
    function resetHistory() { history=[copy(state)]; historyIndex=0; updateHistoryButtons(); }
    function commit() {
        history=history.slice(0,historyIndex+1); history.push(copy(state));
        if (history.length>30) history.shift(); else historyIndex++;
        persist(); updateHistoryButtons();
    }
    function undo() { if (historyIndex<=0) return; historyIndex--; state=copy(history[historyIndex]); render(); }
    function redo() { if (historyIndex>=history.length-1) return; historyIndex++; state=copy(history[historyIndex]); render(); }
    function updateHistoryButtons() {
        if (!studio) return;
        studio.find('.vs-undo').prop('disabled',historyIndex<=0);
        studio.find('.vs-redo').prop('disabled',historyIndex>=history.length-1);
    }

    window.DecoratorVisualStudio = {
        init:function (initOptions) { options=initOptions || {}; mount(); },
        open:open,
        close:close,
        isOpen:isOpen,
        setStatus:setStatus,
        setBusy:setBusy,
        setResults:setResults
    };
})(window, window.jQuery);
