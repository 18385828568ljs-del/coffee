const assert = require('assert')
const fs = require('fs')
const path = require('path')
const vm = require('vm')
const babel = require('@babel/core')

const projectRoot = path.resolve(__dirname, '..')

function loadTabBarUtility() {
	const filename = path.join(projectRoot, 'utils', 'tab-bar.js')
	const source = fs.readFileSync(filename, 'utf8')
	const compiled = babel.transformSync(source, {
		filename,
		plugins: ['@babel/plugin-transform-modules-commonjs']
	}).code
	const module = { exports: {} }
	vm.runInNewContext(compiled, { module, exports: module.exports, getCurrentPages }, { filename })
	return module.exports
}

let route = 'pages/index/index'
global.getCurrentPages = () => [{ route }]

const { syncCustomTabBar } = loadTabBarUtility()
let syncedData = null
const page = {
	getTabBar: () => ({ setData: (data) => { syncedData = data } })
}

route = 'pages/me/me'
assert.strictEqual(syncCustomTabBar({ $mp: { page } }), true)
assert.strictEqual(syncedData.current, 3)

route = 'pages/order/list'
syncedData = null
assert.strictEqual(syncCustomTabBar({ $mp: { page } }), false)
assert.strictEqual(syncedData, null)

let componentDefinition
let switchedTo = null
let switchOptions = null
const componentContext = {
	Component: (definition) => { componentDefinition = definition },
	getCurrentPages: () => [{ route }],
	getApp: () => ({ globalData: {} }),
	wx: {
		getStorageSync: () => '',
		request: () => {},
		switchTab: (options) => {
			switchedTo = options.url
			switchOptions = options
		}
	}
}
const componentFile = path.join(projectRoot, 'custom-tab-bar', 'index.js')
vm.runInNewContext(fs.readFileSync(componentFile, 'utf8'), componentContext, { filename: componentFile })

function componentInstance(data) {
	const instance = {
		data: { ...componentDefinition.data, ...data },
		setData(next) { Object.assign(this.data, next) }
	}
	Object.keys(componentDefinition.methods).forEach((name) => {
		instance[name] = componentDefinition.methods[name].bind(instance)
	})
	return instance
}

route = 'pages/index/index'
const staleInstance = componentInstance({ current: 2 })
staleInstance.selectTab({ currentTarget: { dataset: { index: 2 } } })
assert.strictEqual(switchedTo, '/pages/cart/cart', 'stale visual state must not block the requested route')
assert.strictEqual(staleInstance.data.current, 2, 'selection must not move before navigation completes')

route = 'pages/cart/cart'
switchOptions.complete()
assert.strictEqual(staleInstance.data.current, 2)
assert.strictEqual(staleInstance.data.switching, false)

route = 'pages/index/index'
switchedTo = null
const sameRouteInstance = componentInstance({ current: 3 })
sameRouteInstance.selectTab({ currentTarget: { dataset: { index: 0 } } })
assert.strictEqual(switchedTo, null)
assert.strictEqual(sameRouteInstance.data.current, 0, 'same-route taps must repair stale selection')

console.log('custom tab bar regression tests passed')
