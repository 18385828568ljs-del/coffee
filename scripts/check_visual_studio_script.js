const fs = require('fs')
const path = require('path')

const file = path.resolve(__dirname, '../src/main/resources/static/js/coffee-decorator-visual-studio.js')
const source = fs.readFileSync(file, 'utf8')

new Function(source)

const requiredSnippets = [
    'LEFT_COPY_RIGHT_SUBJECT',
    'RIGHT_COPY_LEFT_SUBJECT',
    'CENTER_SUBJECT',
    'TOP_COPY_BOTTOM_SUBJECT',
    'SCENE_MOOD',
    "slotKey:String(context.slotKey || '')",
    "String(context.slotKey || 'component')",
    'width:Number(context.logicalWidth || 750)',
    'height:Number(context.logicalHeight || 360)',
    "generationMode:'SKETCH'",
    "request.referenceMode='INITIAL_SKIN'",
    'request.referenceAssetId=Number(context.referenceAssetId)',
    "generationType:'BACKGROUND'",
    "textMode:'NO_TEXT'",
    'request.visualIntent=visualIntent()',
    'request.guideImageDataUrl=guideImage()',
    'window.localStorage.setItem',
    "data-action=\"preview\"",
    "data-action=\"accept\"",
    "data-action=\"apply\""
]

requiredSnippets.forEach(snippet => {
    if (!source.includes(snippet)) throw new Error('Visual studio contract is missing: ' + snippet)
})

if (/slotKey\s*:\s*['"]homeBanner['"]/.test(source)) {
    throw new Error('Visual studio must not be hard-coded to homeBanner')
}

const workbench = fs.readFileSync(path.resolve(__dirname, '../src/main/resources/templates/coffee/decorator/workbench.html'), 'utf8')
if (!workbench.includes("$('#skinAiOpenBtn').html('<i class=\"fa fa-magic\"></i> AI 设计背景')")) {
    throw new Error('Background components must use the unified AI design entry')
}
if (!workbench.includes("$('#skinAiPanel').prop('hidden',!productImageEditor")) {
    throw new Error('The legacy inline AI editor must be limited to product images')
}
if (/activeSkinComponent\(\)\.key===['"]homeBanner['"]\) return openVisualStudio/.test(workbench)) {
    throw new Error('AI visual studio must open for every background component')
}

console.log('visual studio script contract ok')
