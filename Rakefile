SOURCES = {'coffeescripts' => 'javascripts', 'spec' => 'spec-js'}
# To enable libnotify (not required), make sure that
#   sudo apt-get install libinotify-ruby libgtk2-ruby libnotify-dev
#    and
#   gem install libnotify

JCOFFEESCRIPT = "https://github.com/downloads/yeungda/jcoffeescript/jcoffeescript-1.1.jar"
COFFEE_JAR = JCOFFEESCRIPT[/\/([^\/]+)$/, 1]

begin
  require 'rubygems'
  require 'libnotify'
  require 'digest/sha1'
rescue Exception
  nil
end
begin
  require 'ftools'
rescue Exception
  nil
end

task :clean do
  SOURCES.values.each do |target_dir|
    `rm -rf #{target_dir}` if(File.exists?(target_dir))
  end
end

task :prepare_tmp do
  `mkdir -p tmp/js_cache tmp/js_error`
end

task :ensure_jcoffeescript => :prepare_tmp do
  Dir.chdir 'tmp' do
    if(Dir.glob('jcoffee*').empty?)
      `wget #{JCOFFEESCRIPT} --no-check-certificate`
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

def source_files
  ret = {}
  SOURCES.keys.each do |k|
    Dir.glob("#{k}/**/*.coffee").each do |fn|
      ret[fn] = fn.sub(/^#{k}/,SOURCES[k]).sub(/\.coffee$/,'.js')
    end
  end
  ret
end

def notify(summary, body)
  begin
    if(defined?(Libnotify))
      Libnotify.show :summary => summary, :body => body
    end
  rescue Exception => e
    puts "Libnotify could not be called #{e.message}"
  end
  puts(summary + ": " + body)
end

def build(src_path, target_path)
  hash = Digest::SHA1.file(src_path).hexdigest
  if(File.exists?('tmp/js_cache/'+hash))
    notify("Coffeescript Built", "Built #{src_path} from cache")
  elsif(File.exists?('tmp/js_error/'+hash))
    s = File.read('tmp/js_error/' + hash)
    notify("Coffeescript ERROR", "Cached error for #{src_path}:\n" + s)
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
      notify("Coffeescript Built", "Finished compiling #{src_path}")
    rescue Exception => e
      # Fail
      File.delete("tmp/js_cache/#{hash}") if File.exists?("tmp/js_cache/#{hash}")
      notify("Coffeescript ERROR", "Build error in #{src_path}:\n" + e.to_s)
      open("tmp/js_error/#{hash}", 'w'){|f|f.write e.to_s}
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

task :build => [:clean, :ensure_jcoffeescript] do
  source_files.each_pair do |src,target|
    build(src,target)
  end
end

task :watch => :ensure_jcoffeescript do
  def get_updates(fns)
    fns.map{|fn|{fn => File.mtime(fn)}}.inject({},&:merge)
  end
  files = source_files
  updates = get_updates(files.keys)
  files.each_pair{|src,target|build(src,target)}
  while true
    files = source_files
    u2 = get_updates(files.keys)
    u2.keys.each do |k|
      build(k, files[k]) unless u2[k] == updates[k]
    end
    updates = u2
    sleep(0.3)
  end
end

task :default => :build
