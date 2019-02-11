const download = require('download-git-repo')
const chalk = require('chalk')

module.exports = (pageName = '', options) => {
  download(
    'direct:ssh://',
    getPath(pageName, options),
    { clone: true },
    err => {
      if (err)
        console.log(`
      we failed, maybe some reasons follow
        ${chalk.yellow(`
        1. check out whether the target folder 【${pageName}】already exit.
        2. maybe some auth problems【gitlab】....
      `)}
      `)
      else
        console.log(`
        ${chalk.blue(`
        we got the new Page! just do it!
        `)}
      `)
    }
  )
}

function getPath(pageName, { prefixPath } = options) {
  let result = pageName
  if (typeof prefixPath === 'string') result = prefixPath + result
  return result
}
