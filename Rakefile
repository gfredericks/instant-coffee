SOURCES = {'coffeescripts' => 'javascripts', 'spec' => 'spec-js'}
# To enable libnotify (not required), make sure that
#   sudo apt-get install libinotify-ruby libgtk2-ruby libnotify-dev
#    and
#   gem install libnotify

JCOFFEESCRIPT = "http://cloud.github.com/downloads/yeungda/jcoffeescript/jcoffeescript-1.1.jar"
COFFEE_JAR = JCOFFEESCRIPT[/\/([^\/]+)$/, 1]

require 'digest/sha1'
require 'net/http'

begin
  require 'rubygems'
  require 'libnotify'
rescue Exception
  nil
end
begin
  require 'ftools'
rescue Exception
  nil
end

module InstantCoffeeRakeHelper
  def self.source_files
    ret = {}
    SOURCES.keys.each do |k|
      Dir.glob("#{k}/**/*.coffee").each do |fn|
        ret[fn] = fn.sub(/^#{k}/,SOURCES[k]).sub(/\.coffee$/,'.js')
      end
    end
    ret
  end

  def self.notify(summary, body)
    begin
      if(defined?(Libnotify))
        Libnotify.show :summary => summary, :body => body
      end
    rescue Exception => e
      puts "Libnotify could not be called #{e.message}"
    end
    puts(summary + ": " + body)
  end

  def self.build(src_path, target_path)
    hash = Digest::SHA1.file(src_path).hexdigest
    if(File.exists?('tmp/js_cache/'+hash))
      self.notify("Coffeescript Built", "Built #{src_path} from cache")
    elsif(File.exists?('tmp/js_error/'+hash))
      s = File.read('tmp/js_error/' + hash)
      self.notify("Coffeescript ERROR", "Cached error for #{src_path}:\n" + s)
    else
      print "Building #{src_path}... to tmp/js_cache/#{hash}"
      begin
        if($jcsc)
          coffee = File.read(src_path)
          js = $jcsc.compile(coffee)
          open("tmp/js_cache/#{hash}",'w'){|f|f.write(js)}
        else
          system "java -jar tmp/jcoffeescript-1.1.jar --bare < #{src_path} > tmp/js_cache/#{hash}"
        end
        self.notify("Coffeescript Built", "Finished compiling #{src_path}")
      rescue Exception => e
        # Fail
        File.delete("tmp/js_cache/#{hash}") if File.exists?("tmp/js_cache/#{hash}")
        self.notify("Coffeescript ERROR", "Build error in #{src_path}:\n" + e.to_s)
        open("tmp/js_error/#{hash}", 'w'){|f|f.write e.to_s}
        File.delete(target_path) if File.exists?(target_path)
      end
    end
    if(File.exists?("tmp/js_cache/#{hash}"))
      src_dir = SOURCES.keys.find{|k|src_path =~ /^#{k}/}
      if(target_path =~ /^(.*)\/[^\/]+\.js$/)
        `mkdir -p #{$1}`
      end
      File.delete(target_path) if File.exists?(target_path)
      ab_path = if File.respond_to?(:absolute_path)
          File.absolute_path('tmp/js_cache/'+hash)
        else
          File.expand_path('tmp/js_cache/'+hash)
        end
      File.link(ab_path, target_path)
    end
  end

  # Retrieves uri over https and saves to filename
  def self.http_get(uri, filename)
    uri = URI.parse(uri)
    res = Net::HTTP.start(uri.host, uri.port){|http|
      http.get uri.path
    }
    open(filename,'w'){|f|f.write(res.body)}
  end
end

namespace :instant_coffee do

  desc "Remove all target directories"
  task :clean do
    SOURCES.values.each do |target_dir|
      if(File.exists? target_dir)
        Dir.glob(target_dir + '/**/*.js').each{|x|
          puts("Deleting #{x}...")
          File.delete(x)
        }
        remaining = Dir.entries(target_dir) - ['.','..']
        if(remaining.empty?)
          puts("Deleting #{target_dir}")
          File.delete target_dir
        end
      end
    end
  end

  desc "Ensure existence of tmp directories"
  task :prepare_tmp do
    `mkdir -p tmp/js_cache tmp/js_error`
  end

  desc "Ensure presence of jcoffeescript jar"
  task :ensure_jcoffeescript => :prepare_tmp do
    Dir.chdir 'tmp' do
      if(Dir.glob('jcoffee*').empty?)
        InstantCoffeeRakeHelper.http_get(JCOFFEESCRIPT, COFFEE_JAR)
      end
    end
    if(defined? Java)
      puts "Calling java directly enabled..."
      require "tmp/#{COFFEE_JAR}"
      $jcsc = Java::OrgJcoffeescript::JCoffeeScriptCompiler.new([Java::OrgJcoffeescript::Option::BARE])
    else
      puts "Not using jruby, will have to shell out..."
    end
  end

  desc "Build all src files"
  task :build => [:clean, :ensure_jcoffeescript] do
    InstantCoffeeRakeHelper.source_files.each_pair do |src,target|
      InstantCoffeeRakeHelper.build(src,target)
    end
  end

  desc "Build all src files and watch for changes"
  task :watch => :ensure_jcoffeescript do
    get_updates = lambda do |fns|
      fns.map{|fn|{fn => File.mtime(fn)}}.inject({},&:merge)
    end
    files = InstantCoffeeRakeHelper.source_files
    updates = get_updates.call(files.keys)
    files.each_pair{|src,target|InstantCoffeeRakeHelper.build(src,target)}
    while true
      files2 = InstantCoffeeRakeHelper.source_files
      u2 = get_updates.call(files2.keys)
      u2.keys.each do |k|
        InstantCoffeeRakeHelper.build(k, files2[k]) unless u2[k] == updates[k]
      end
      # deleted files
      (updates.keys - u2.keys).each do |deleted_file|
        File.delete(files[deleted_file])
      end
      updates = u2
      files = files2
      sleep(0.3)
    end
  end
end
