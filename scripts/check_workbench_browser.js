const { chromium } = require('playwright');
const readline = require('readline');

const baseUrl = process.env.WORKBENCH_QA_BASE_URL || 'http://127.0.0.1:18223';
const username = process.env.WORKBENCH_QA_USERNAME;
const password = process.env.WORKBENCH_QA_PASSWORD;
const outputDir = process.env.WORKBENCH_QA_OUTPUT || 'target/workbench-qa';
const browserPath = process.env.WORKBENCH_QA_BROWSER || chromium.executablePath();
const storagePath = outputDir + '/auth-state.json';
const createName = process.env.WORKBENCH_QA_CREATE_NAME;
const createSource = process.env.WORKBENCH_QA_CREATE_SOURCE;

if (!username || !password) {
    throw new Error('WORKBENCH_QA_USERNAME and WORKBENCH_QA_PASSWORD are required');
}

function readCaptcha() {
    const input = readline.createInterface({ input: process.stdin, output: process.stdout });
    return new Promise(resolve => input.question('captcha: ', value => {
        input.close();
        resolve(value.trim());
    }));
}

(async () => {
    const fs = require('fs');
    fs.mkdirSync(outputDir, { recursive: true });
    const browser = await chromium.launch({ headless: true, executablePath: browserPath });
    try {
        const context = await browser.newContext({
            viewport: { width: 1440, height: 960 },
            storageState: fs.existsSync(storagePath) ? storagePath : undefined
        });
        const page = await context.newPage();
        const pageErrors = [];
        const consoleErrors = [];
        page.on('pageerror', error => pageErrors.push(error.message));
        page.on('console', message => {
            if (message.type() === 'error') consoleErrors.push(message.text());
        });

        await page.goto(baseUrl + '/coffee/decorator/workbench', { waitUntil: 'networkidle' });
        if (page.url().includes('/login')) {
            const captchaPath = outputDir + '/captcha.png';
            await page.locator('.imgcode').screenshot({ path: captchaPath });
            process.stdout.write('captchaImage=' + captchaPath + '\n');
            const captcha = await readCaptcha();
            await page.locator('[name="username"]').fill(username);
            await page.locator('[name="password"]').fill(password);
            await page.locator('[name="validateCode"]').fill(captcha);
            await page.locator('#btnSubmit').click();
            await page.waitForTimeout(1200);
            if (page.url().includes('/login')) {
                const message = await page.locator('.layui-layer-content').last().textContent().catch(() => '登录失败');
                throw new Error(message || '登录失败');
            }
            await context.storageState({ path: storagePath });
            await page.goto(baseUrl + '/coffee/decorator/workbench', { waitUntil: 'networkidle' });
        }

        await page.locator('#merchantSelect').waitFor({ state: 'visible' });
        await page.waitForFunction(() => !document.querySelector('#newThemeBtn').disabled);
        const before = await page.evaluate(() => ({
            merchant: document.querySelector('#merchantSelect option:checked').textContent.trim(),
            scope: document.querySelector('#storeSelect option:checked').textContent.trim(),
            theme: document.querySelector('#themeSelect option:checked').textContent.trim()
        }));
        await page.locator('#newThemeBtn').click();
        await page.waitForTimeout(800);
        const result = await page.evaluate(() => {
            const name = document.querySelector('#newThemeName');
            const source = document.querySelector('#newThemeSource option:checked');
            const submit = document.querySelector('#submitNewTheme');
            return {
                dialogExists: !!name,
                dialogVisible: !!name && name.offsetParent !== null,
                sourceText: source ? source.textContent.trim() : null,
                createDisabled: submit ? submit.disabled : null,
                openLayers: document.querySelectorAll('.layui-layer').length,
                pageTitle: document.title
            };
        });
        await page.screenshot({ path: outputDir + '/new-theme-dialog.png', fullPage: true });
        const checks = {};
        if (createName) {
            await page.locator('#newThemeName').fill(createName);
            if (createSource) {
                const sourceValue = await page.locator('#newThemeSource option').evaluateAll((options, text) => {
                    const option = options.find(item => item.textContent.includes(text));
                    return option ? option.value : null;
                }, createSource);
                if (!sourceValue) throw new Error('Theme source was not found: ' + createSource);
                await page.locator('#newThemeSource').selectOption(sourceValue);
            }
            const responsePromise = page.waitForResponse(response => response.request().method() === 'POST'
                && response.url().endsWith('/coffee/decorator/themes'));
            await page.locator('#submitNewTheme').click();
            const response = await responsePromise;
            const responseBody = await response.json();
            await page.waitForTimeout(1200);
            result.create = {
                httpStatus: response.status(),
                code: responseBody.code,
                message: responseBody.msg,
                dialogVisible: await page.locator('#newThemeName').isVisible().catch(() => false),
                inlineError: await page.locator('#newThemeError').textContent().catch(() => null),
                selectedTheme: await page.locator('#themeSelect option:checked').textContent()
            };
            await page.screenshot({ path: outputDir + '/create-theme-result.png', fullPage: true });
        }
        if (await page.locator('#cancelNewTheme').isVisible().catch(() => false)) await page.locator('#cancelNewTheme').click();
        await page.waitForFunction(() => document.querySelectorAll('.layui-layer').length === 0);
        checks.cancelClosed = true;

        await page.locator('#versionsBtn').click();
        await page.locator('#versionDialogRows').waitFor({ state: 'visible' });
        checks.versionsDialog = await page.locator('#versionDialogRows tr').count() > 0;
        await page.locator('.layui-layer-close').last().click();

        await page.locator('#validateBtn').click();
        await page.waitForFunction(() => document.querySelector('#editorError').textContent.includes('配置校验通过'));
        checks.validate = true;

        const followingStoreId = await page.evaluate(() => {
            const option = Array.from(document.querySelectorAll('#storeSelect option'))
                .find(item => item.textContent.includes('· 跟随'));
            return option ? option.value : null;
        });
        if (followingStoreId) {
            await page.locator('#storeSelect').selectOption(followingStoreId);
            await page.waitForFunction(() => window.state && state.scopeReadOnly === true
                && document.querySelector('#saveBtn').disabled
                && document.querySelector('#publishBtn').disabled
                && document.querySelector('#previewBtn').disabled);
            checks.followingStoreReadOnly = await page.evaluate(() => ({
                noticeVisible: document.querySelector('#scopeNotice').offsetParent !== null,
                independentEntryEnabled: !document.querySelector('#scopeNoticeIndependentBtn').disabled,
                saveDisabled: document.querySelector('#saveBtn').disabled,
                publishDisabled: document.querySelector('#publishBtn').disabled,
                previewDisabled: document.querySelector('#previewBtn').disabled
            }));
            await page.locator('#storeSelect').selectOption('master');
            await page.waitForFunction(() => window.state && state.scopeReadOnly === false);
        }

        result.before = before;
        result.checks = checks;
        result.pageErrors = pageErrors;
        result.consoleErrors = consoleErrors;
        process.stdout.write(JSON.stringify(result) + '\n');
        const readonly = checks.followingStoreReadOnly;
        const readonlyPassed = !readonly || Object.values(readonly).every(Boolean);
        const createPassed = !createName || result.create && Number(result.create.code) === 0
            && !result.create.dialogVisible && result.create.selectedTheme.includes(createName);
        if (!result.dialogVisible || !checks.cancelClosed || !checks.versionsDialog || !checks.validate
                || !readonlyPassed || !createPassed || result.pageErrors.length) process.exitCode = 1;
    } finally {
        await browser.close();
    }
})().catch(error => {
    process.stderr.write(error.stack + '\n');
    process.exitCode = 1;
});
