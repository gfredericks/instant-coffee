# Instant Coffee Sample Project

This is a sample project to give some guidance on how to use it as a development tool.

## Getting Started

Install the dependencies. Here are some links to find out more about using [RVM](www.beginrescueend.com) and [Bundler](gembundler.com). This application comes with a sample Gemfile for use with bundler.

    rvm use someruby@sample_project --create
    gem install bundler
    bundle install

To see all the available rake tasks you can run:

    rake -T
    
If you want to take advantage of the 'rake develop' task, we recommend using jruby. This will use native java threads, compiling your coffeescript and serving your tests with the jasmine gem.

    rvm install jruby
    rvm use jruby@myproject --create
    rake develop
    
