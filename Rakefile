require 'rake/packagetask'
require 'rexml/document'
require 'erb'


pom = REXML::Document.new File.new("pom.xml")
name =  pom.elements["project"].elements["name"].text
version =  pom.elements["project"].elements["version"].text
name_ver = "#{name}-#{version}"
jar = "#{name_ver}-jar-with-dependencies.jar"
tar = "#{name_ver}.tar.gz"
fuse4j = '../fuse4j/native/'

def server_path 
	(`uname -a`.include?("i686") && "i386") || "amd64"
end

def mvn_with_ld(goal)
	ENV["LD_LIBRARY_PATH"]="/usr/lib:#{ENV['JAVA_HOME']}/jre/lib/#{server_path}/server:#{pwd}/../fuse4j/native"
	sh "mvn #{goal}"
end


desc "maven clean install with ld path"
task :mvnci => :logfile  do
	mvn_with_ld 'clean install -o'
end

desc "maven compile and test with ld path"
task :compile => :logfile  do
	mvn_with_ld 'clojure:compile -o'
	mvn_with_ld 'clojure:test -o'
end

task :default => [:package]

task :logfile do
	sh 'sudo touch /var/log/couchfuse.log'
	sh 'sudo chmod 666 /var/log/couchfuse.log'
end

desc "create jar using maven assembly plugin"
file jar => ['native/javafs',:logfile] do
	mkdir 'fake' unless File.exists? 'fake'
	mvn_with_ld('assembly:assembly') unless File.exists? "target/#{jar}"
	cp "target/#{jar}" , pwd
	cp 'target/classes/log4j.properties' , pwd
end 

file 'couchfuse' do
	path = "/usr/lib/jvm/java-6-sun/jre/lib/#{server_path}/server"
	launch = "java -Dlog4j.configuration='file:/usr/share/couchfuse/log4j.properties' -Djava.library.path=/usr/lib:#{path}:/usr/share/couchfuse/native -jar /usr/share/couchfuse/#{jar} '#{name_ver} filesystem' $@"
	script = "" 
	File.open('packaging/couchfuse.bin' , 'r') { |f| script = f.read }
	template = ERB.new(script)
	File.open('couchfuse' , 'w') do |f|  f.puts template.result(binding) end       
end

file 'native/javafs','native/libjavafs.so' => :fusebuild do
	mkdir 'native' unless File.exists? 'native'
	%w(javafs  libjavafs.so).each {|f| cp "#{fuse4j}#{f}" , 'native'}
end

Rake::PackageTask.new(name, version)  do |pack|
	pack.need_tar_gz = true
	pack.package_files.include(jar,'couchfuse','native/javafs','native/libjavafs.so','log4j.properties')
end

task :fusebuild do
	origin = pwd
	chdir fuse4j
	sh 'rake clean && rake'
	chdir origin
end

task :clean  do
	([jar] + %w(pkg javafs native couchfuse log4j.properties)).each {|f| rm_r f if File.exists? f}
	sh 'sudo rm -r sandbox' if File.exists? 'sandbox'
	sh 'mvn clean'
end

desc "builds the deb package"
task :deb => [:clean, :sandbox] do 
	['control','rules','dirs','postinst','prerm'].each{|f| cp "../../packaging/debian/#{f}",'debian/' } 
	sh 'sudo dpkg-buildpackage'
end

desc "build the deb sandbox folder"
task :sandbox => [:package] do
	mkdir('sandbox') unless File.exists?('sandbox')
	cp "pkg/#{tar}" , 'sandbox'
	cd 'sandbox'
	sh "tar -xvzf #{tar}"
	mv tar , name_ver
	cd name_ver
	sh "echo 'skip confirmation' | dh_make -e narkisr.dev@gmail.com -c apache -f #{tar} -s -p #{name}_#{version}"
	rm "../#{name}_#{version}.orig.tar.gz"
	Dir['debian/*.ex'].each {|fn| rm fn rescue nil}
end