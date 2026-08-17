const fs = require('fs')
const path = require('path')

const file = path.resolve(__dirname, '../src/main/resources/templates/coffee/decorator/workbench.html')
const html = fs.readFileSync(file, 'utf8')
const marker = '<script th:inline="javascript">'
const start = html.indexOf(marker)
const end = html.indexOf('</script>', start)

if (start < 0 || end < 0) throw new Error('Workbench inline script was not found')

const source = html.slice(start + marker.length, end)
new Function(source)

if (/content\s*:\s*content(?:\[0\]|\s*[,}])/.test(source)) {
    throw new Error('Layer dialogs must receive serialized HTML content')
}

const backgroundEditorStart = source.indexOf('function renderPageBackgroundEditor()')
const backgroundEditorEnd = source.indexOf('\n}\n$(function()', backgroundEditorStart)
if (backgroundEditorStart < 0 || backgroundEditorEnd < 0) {
	throw new Error('Page background editor function was not found')
}
const backgroundEditorBody = source.slice(backgroundEditorStart, backgroundEditorEnd)
if (/renderPageBackgroundEditor\s*\(\s*\)/.test(backgroundEditorBody.replace('function renderPageBackgroundEditor()', ''))) {
	throw new Error('Page background editor must not call itself')
}

console.log('workbench script syntax ok')
