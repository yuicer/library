#!/usr/bin/env node

// https://github.com/tj/commander.js
// https://github.com/SBoudrias/Inquirer.js
'use strict'

const chalk = require('chalk')
const program = require('commander')

program.version('1.0.0').usage('<command> [options]')

// create new project with @vue/cli3
program
  .command('create <projectName>')
  .option('-m, --mservice', 'create mservice vue-tpl')
  .option('-w, --webpack', 'create webpack vue-tpl')
  .description('create a new project powered by vue-cli-service')
  .action((projectName, cmd) => {
    const options = cleanArgs(cmd)
    require('../lib/createTpl')(projectName, options)
  })
  .on('--help', () => {
    console.log(
      `\n  example:
       \n    ${chalk.cyan(`cli create <projectName>`)} 
      \n  to create the new project for mservices\n`
    )
  })

// generate new page for mpa project 【need extra tpl provided】
program
  .command('new <pageName>')
  .option('-p, --prefixPath [path]', 'set prefixPath')
  .description('create a new page for mpa project')
  .action((pageName, cmd) => {
    const options = cleanArgs(cmd)
    require('../lib/newPage')(pageName, options)
  })
  .on('--help', () => {
    console.log(
      `\n  example:
       \n    ${chalk.cyan(`cli new <pageName>`)} 
      \n  to generate the new page for mpa project\n`
    )
  })

// output help information on unknown commands
program.arguments('<command>').action(cmd => {
  program.outputHelp()
  console.log(`\n  ` + chalk.red(`Unknown command ${chalk.yellow(cmd)}.`))
})

// add some useful info on help
program.on('--help', () => {
  console.log(
    `\n  \n  Run ${chalk.cyan(
      `cli <command> --help`
    )} for detailed usage of given command.\n`
  )
})
program.parse(process.argv)

// output help when type cli
if (!process.argv.slice(2).length) {
  program.outputHelp()
}

// commander passes the Command object itself as options,
// extract only actual options into a fresh object.
function cleanArgs(cmd) {
  const args = {}
  cmd.options.forEach(o => {
    const key = camelize(o.long.replace(/^--/, ''))
    // if an option is not present and Command has a method with the same name
    // it should not be copied
    if (typeof cmd[key] !== 'function' && typeof cmd[key] !== 'undefined') {
      args[key] = cmd[cmd[key]] || cmd[key]
    }
  })
  return args
}

function camelize(str) {
  return str.replace(/-(\w)/g, (_, c) => (c ? c.toUpperCase() : ''))
}
