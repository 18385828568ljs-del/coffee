module.exports = {
  plugins: [
    require('postcss-import')(),
    require('@dcloudio/vue-cli-plugin-uni/packages/postcss')(),
    require('autoprefixer')()
  ]
}
