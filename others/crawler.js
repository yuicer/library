const axios = require('axios')
const cheerio = require('cheerio')
const fs = require('fs')
// https://otakism.com/

var imgs_store = []
axios.get('http://liuqjiang.lofter.com/').then(
  res => {
    var $ = cheerio.load(res.data)
    var imgs = $('img')
    imgs.each(function(i, e) {
      imgs_store.push($(e).attr('src'))
    })
    console.log(imgs_store)
    for (var i in imgs_store) {
      if (imgs_store[i].slice(0, 4) !== 'http') {
        imgs_store.splice(i, 1)
      }
    }
    for (let i = 0; i < imgs_store.length; i++) {
      axios
        .get(imgs_store[i], {
          responseType: 'stream'
        })
        .then(
          res1 => {
            res1.data.pipe(fs.createWriteStream('./img' + i + '.jpg'))
          },
          res1 => {
            console.log(res1)
          }
        )
    }
  },
  res => {
    console.log(res)
  }
)
