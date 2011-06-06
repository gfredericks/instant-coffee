SOURCES = {'coffeescripts' => 'javascripts', 'spec' => 'spec-js'}


# To enable libnotify, make sure that
#   sudo apt-get install libinotify-ruby libgtk2-ruby libnotify-dev
#    and
#   gem install libnotify
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

task :clean do
  SOURCES.values.each do |target_dir|
    `rm -rf #{target_dir}` if(File.exists?(target_dir))
  end
end

task :prepare_tmp do
  `mkdir -p tmp/js_cache`
end

task :ensure_jcoffeescript => :prepare_tmp do
  Dir.chdir 'tmp' do
    if(Dir.glob('jcoffee*').empty?)
      `wget https://github.com/downloads/yeungda/jcoffeescript/jcoffeescript-1.0.jar --no-check-certificate`
    end
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
  if(defined?(Libnotify))
    Libnotify.show :summary => summary, :body => body
  end
end

def build(src_path, target_path)
  hash = `sha1sum #{src_path}`.split[0]
  unless(File.exists?('tmp/js_cache/'+hash))
    print "Building #{src_path}..."
    `java -jar tmp/jcoffeescript-1.0.jar --bare < #{src_path} > tmp/js_cache/#{hash} 2> tmp/jcs.error`
    e = File.read 'tmp/jcs.error'
    print("\n")
    if(e.strip.empty?)
      # Success
      notify("Coffeescript Built", "Finished compiling #{src_path}")
    else
      # Fail
      notify("Coffeescript ERROR", "Build error in #{src_path}:\n" + e)
      puts(e)
      File.delete('tmp/js_cache/'+hash)
    end
  else
    notify("Coffeescript Built", "Built #{src_path} from cache")
  end
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
