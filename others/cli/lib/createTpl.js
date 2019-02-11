const path = require('path')
const vuePath = path.join(__dirname, '../node_modules', '.bin', 'vue')
const { execFileSync } = require('child_process')

module.exports = (projectName, options) => {
  const arr = getCommand(projectName, options).split(' ')

  return execFileSync(vuePath, arr.slice(1), {
    cwd: path.join(process.cwd()),
    stdio: 'inherit'
  })
}

function getCommand(projectName, options) {
  const commands = {
    mservice: `vue create -p direct:ssh://--clone ${projectName}`,
    webpack: `vue init direct:ssh://--clone ${projectName}`
  }

  for (let key in options) {
    if (options[key]) return commands[key]
  }
  return commands['mservice']
}
