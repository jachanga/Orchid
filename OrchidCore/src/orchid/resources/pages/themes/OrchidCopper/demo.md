---
title: Copper Demo
theme: Copper
layout: homepage
template: homepage
skipTaxonomy: true

dashboardTiles:
- title: ''
  parentNodes:
    - width: 4
      vertical: true
      childrenNodes:
        - link: ':githubProject(copper-leaf/trellis)'
          color: success
    - width: 8
      vertical: true
      childrenNodes:
        - link: ':githubProject(copper-leaf/clog)'
          color: primary
- title: ''
  parentNodes:
    - width: 4
      vertical: true
      childrenNodes:
        - link: ':githubProject(copper-leaf/kudzu)'
          color: info
        - link: ':githubProject(copper-leaf/krow)'
          color: primary
    - width: 8
      vertical: true
      childrenNodes:
        - link: ':githubProject(JavaEden/Orchid)'
          color: danger
- title: ''
  parentNodes:
    - width: 12
      vertical: true
      childrenNodes:
        - title: Latest Blog Post
          color: info
          link: ':latestBlogPost()'
- title: ''
  parentNodes:
    - width: 4
      vertical: true
      childrenNodes:
        - title: Donec lobortis
          subtitle: Ut tempor sed nisi et scelerisque. Nam.
          color: success
          link: '#'
        - title: Etiam pellentesque
          subtitle: Aliquam vitae metus at ante sagittis tristique.
          color: info
          link: '#'
    - width: 4
      vertical: true
      childrenNodes:
        - title: Suspendisse potenti
          subtitle: Aliquam a ex non erat ultricies egestas.
          color: primary
          link: '#'
    - width: 4
      vertical: true
      childrenNodes:
        - title: Donec ai
          subtitle: Class aptent taciti sociosqu ad litora torquent.
          color: success
          link: '#'
        - title: Ut erat
          subtitle: Donec malesuada dui et sem congue mollis.
          color: info
          link: '#'
---

{% tiles tiles=(dashboardTiles) %}