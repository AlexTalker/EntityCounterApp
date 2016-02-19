# EntityCounterApp [![Build Status](https://travis-ci.org/AlexTalker/EntityCounterApp.svg?branch=master)](https://travis-ci.org/AlexTalker/EntityCounterApp)

Weird English included.

Simple app to calculate valid html entities from files, directories or urls.

Based on [HTML5 8.2.4.69 Tokenizing character reference](http://www.w3.org/TR/html5/syntax.html#additional-allowed-character) and browser experience. Works mostly-like Firefox do.

## Requirements
- `jackson` to build prefix tree from HTML5 entities list(json file). It's already in `jars/`.

## Bugs
- `URL.openStream()` sometimes may stuck. We have `Ctrl-C` to handle this :)
- No, you cannot calculate entities on pages(for example, by HTTP) which return non-OK status code. Just because.
- Maybe I miss something. Seems like standard and browsers have different thougths about what's right and what's wrong.

No, there's no JUnit or smth like that. Just believe that it calculates right.
