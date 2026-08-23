const loadedFonts = Object.create(null)

function loadWithFontFace(familyName, url, descriptors) {
	const face = new FontFace(familyName, `url("${url}")`, descriptors)
	return face.load().then((loaded) => {
		document.fonts.add(loaded)
		return true
	})
}

function loadWithUni(familyName, url) {
	return new Promise((resolve, reject) => {
		uni.loadFontFace({
			global: true,
			family: familyName,
			source: `url("${url}")`,
			success: () => resolve(true),
			fail: reject
		})
	})
}

export function loadFontResource(resource) {
	if (!resource || !resource.familyName || !resource.url) return Promise.resolve(false)
	const key = [resource.familyName, resource.url, resource.weight || 400, resource.style || 'normal'].join('|')
	if (loadedFonts[key]) return loadedFonts[key]
	let task
	if (typeof FontFace !== 'undefined' && typeof document !== 'undefined' && document.fonts) {
		task = loadWithFontFace(resource.familyName, resource.url, {
			weight: String(resource.weight || 400),
			style: resource.style || 'normal'
		})
	} else if (typeof uni !== 'undefined' && uni.loadFontFace) {
		task = loadWithUni(resource.familyName, resource.url)
	} else {
		task = Promise.resolve(false)
	}
	loadedFonts[key] = task.catch(() => false)
	return loadedFonts[key]
}

export function loadFontResources(resources = {}) {
	return Promise.all(Object.keys(resources).map((fontId) => loadFontResource(resources[fontId])))
}
