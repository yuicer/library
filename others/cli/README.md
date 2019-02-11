## usage

```shell
  npm i @yuicer/cli
  cli --help
```

## feature

`cli create`

```js
program
  .command('create <projectName>')
  .option('-m, --mservice', 'create mservice vue-tpl')
  .option('-w, --webpack', 'create webpack vue-tpl')
  .description('create a new project powered by vue-cli-service')
```

`cli new`

```js
program
  .command('new <pageName>')
  .option('-p, --prefixPath [path]', 'set prefixPath')
  .description('create a new page for mpa project')
```
