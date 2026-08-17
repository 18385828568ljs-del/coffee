const fs = require('fs')
const path = require('path')

const loaderPath = path.join(
  __dirname,
  '..',
  'node_modules',
  '@dcloudio',
  'vue-cli-plugin-uni',
  'packages',
  'vue-loader',
  'lib',
  'loaders',
  'templateLoader.js'
)

if (!fs.existsSync(loaderPath)) process.exit(0)

const source = fs.readFileSync(loaderPath, 'utf8')
let patched = source
  .replace(
    '  return code + `\\nexport { render, staticRenderFns, recyclableRender, components }`',
    '  return `const recyclableRender = undefined\\nconst components = undefined\\n${code}\\nexport { render, staticRenderFns, recyclableRender, components }`'
  )
  .replace(
    '  return code + `\\nexport { render, staticRenderFns, components }`',
    '  return `const recyclableRender = undefined\\nconst components = undefined\\n${code}\\nexport { render, staticRenderFns, recyclableRender, components }`'
  )
if (patched.includes('return code + `\\nexport { render, staticRenderFns, components }`')) process.exit(0)
if (patched !== source) fs.writeFileSync(loaderPath, patched, 'utf8')
