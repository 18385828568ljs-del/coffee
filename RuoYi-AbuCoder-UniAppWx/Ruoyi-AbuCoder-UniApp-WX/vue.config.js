module.exports = {
  transpileDependencies: ['uview-ui', /node_modules[\\/]@dcloudio[\\/]/],
  devServer: {
    proxy: {
      '/api': { target: process.env.H5_API_PROXY || 'http://127.0.0.1:8080', changeOrigin: true },
      '/wxapi': { target: process.env.H5_API_PROXY || 'http://127.0.0.1:8080', changeOrigin: true },
      '/profile': { target: process.env.H5_API_PROXY || 'http://127.0.0.1:8080', changeOrigin: true },
      '/common': { target: process.env.H5_API_PROXY || 'http://127.0.0.1:8080', changeOrigin: true }
    }
  }
}
