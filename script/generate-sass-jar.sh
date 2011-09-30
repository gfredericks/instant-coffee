#!/usr/bin/env sh

mkdir sass-gems
java -jar lib/jruby-complete-1.6.4.jar -S gem install -i ./sass-gems sass --no-rdoc --no-ri
jar cf sass-gems.jar -C sass-gems .
rm -rf sass-gems
