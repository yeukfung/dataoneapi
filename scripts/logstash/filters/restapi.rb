# Call this file 'foo.rb' (in logstash/filters, as above)

require "logstash/filters/base"
require "logstash/namespace"

class LogStash::Filters::Restapi < LogStash::Filters::Base

  # Setting the config_name here is required. This is how you
  # configure this filter from your logstash config.
  #
  config_name "restapi"

  # New plugins should start life at milestone 1.
  milestone 1

  # Replace the message with this value.
  config :url, :validate => :string

  public
  def register
    require 'rubygems'

    # need to run this command to install local gem to bundle: 
    # env GEM_HOME=vendor/bundle/jruby/1.9/ GEM_PATH="/Library/Ruby/Gems/1.8" java -jar vendor/jar/jruby-complete-1.7.11.jar -S gem install api_cache    
    require 'api_cache'
    

  end # def register

  public
  def filter(event)
    # return nothing unless there's an actual filter event
    return unless filter?(event)

    if @url
      url = event.sprintf(@url)
      response = APICache.get(url)
      event["resp"] = response.encode("UTF-8")
    end

    # filter_matched should go in the last line of our successful code 
    filter_matched(event)
  end # def filter
end # class LogStash::Filters::Foo