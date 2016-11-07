# How to filter (some) advertising with linux and dnsmasq

[![Build Status](https://travis-ci.org/lcappuccio/filtering-ads-with-linux.svg?branch=master)](https://travis-ci.org/lcappuccio/filtering-ads-with-linux)
[![codecov](https://codecov.io/gh/lcappuccio/filtering-ads-with-linux/branch/master/graph/badge.svg)](https://codecov.io/gh/lcappuccio/filtering-ads-with-linux)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b6cf8eb58c5c441eaaa52a79b6f91bf7)](https://www.codacy.com/app/lcappuccio/filtering-ads-with-linux?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=lcappuccio/filtering-ads-with-linux&amp;utm_campaign=Badge_Grade)
[![codebeat badge](https://codebeat.co/badges/420cede9-dd38-4629-ac71-0d377143553b)](https://codebeat.co/projects/github-com-lcappuccio-filtering-ads-with-linux)

A brief recap about how to filter (some) ads with a linux box. This will not remove all ads but can greatly help if used with AdBlock, uBlockOrigin, Ghostery, etc.

The idea is very simple and very well documented on the 'net:

*Use dnsmasq to point a list of domains to a local webserver that will replace the ads with a 1px transparent gif.*

To obtain this we will need:
* a linux machine or a raspberry (I'm using a Pi3)
* dnsmasq installed
* a local webserver (I'm using lighttpd)
* a running mysql database

Nice to have:
* dhcp on the same box
* some reporting or statistics
* logging

## Installing dnsmasq

This is basically a matter of typing:

`sudo apt-get install dnsmasq`

### Configuring dnsmasq

See `dnsmasq.conf` and `hosts`. On my example I'm also using this machine as DHCP server and resolving some local hostnames. I'm also using google's DNS.

Example `dnsmasq.conf` (**please be sure to disable dhcp logging, otherwise logtailer will crash**)

```
######### dns ########
# Never forward plain names (without a dot or domain part)
domain-needed
# Never forward addresses in the non-routed address spaces
bogus-priv
# dont read resolv.conf   use the defined servers instead
no-resolv
server=8.8.8.8
server=8.8.4.4
# increase dns cache from 512 to 4096
cache-size=4096

######### dhcp ##########
# Add local-only domains here, queries in these domains are answered
# from /etc/hosts or DHCP only
local=/home/
# Set this (and domain: see below) if you want to have a domain
# automatically added to simple names in a hosts-file.
expand-hosts
# adds my localdomain to each dhcp host
domain=home
# my private dhcp range + subnetmask + 14d lease time
dhcp-range=192.168.0.20,192.168.0.254,255.255.255.0,8h
# set route to my local network router
dhcp-option=option:router,192.168.0.1
#windows 7 float fix
#http://brielle.sosdg.org/archives/522-Windows-7-flooding-DHCP-server-with-DHCPINFORM-messages.html
dhcp-option=252,"\n"

###### logging ############
# own logfile
log-facility=/var/log/dnsmasq.log
log-async
# log dhcp infos
# log-dhcp
# debugging dns
log-queries
```

Everything is on domain `home`. After installing and configuring:

`sudo service dnsmasq restart`

## Configure router

On my home modem/router I had to set the new dnsmasq machine as DNS. Remember to disable also the DHCP server, otherwise disable the DHCP on the dnsmasq machine.

## Configure local webserver

Will not get much into detail here, simply install lighttpd, apache, pixelserv or whatever other web server of your choice. Personally I'm using lighttpd.

On the `lighttpd.conf` file remember to set:

`server.error-handler-404    = "/1px.gif"`

`index-file.names            = ( "1px.gif" )`

This will forward requests to http://127.0.0.1 directly to our 1px gif.

## Configure database

`sudo apt-get install mysql-server`

## Create the lists

Now for the fun part. Actually I'm not creating anything, just putting together pieces that I've found on the web. Inspired by a post about [pihole](https://pi-hole.net) I started to gather information and create a DIY solution to address the same problem:

* A [post](https://www.reddit.com/r/pihole/comments/4p2tp7/adding_easylist_and_other_adblocklike_sources_to/) on reddit
* Look for google *dnsmasq block ads*

The entrypoint is `manual_easylist.sh`. This bash script will download the lists in `lists.lst`, parse them and put them into another file.

If lighttpd is not running on your local machine I replace the `127.0.0.1` on the end of the script with the ip of my ad trap system.

On this same file I'm appending some other hosts found on [URLBlacklist](http://www.urlblacklist.com). After all this parsing and appending the script will sort and remove duplicates.

Finally we will generate `blacklist_dnsmasq.txt`. Copy this file to `/etc/dnsmasq.d` and again restart dnsmasq.

Start browsing and issue
`> sudo tail -f /var/log/dnsmasq.log | grep 127.0.0.1`

And you should see something like this:

`Oct  4 19:40:38 dnsmasq[9669]: config pagead2.googlesyndication.com is 127.0.0.1`

`Oct  4 19:40:38 dnsmasq[9669]: config s7.addthis.com is 127.0.0.1`

So we are redirecting some stuff. :)

## Extras

* If using DHCP I've prepared a php page listing all leases
* Bandwidthd monitor (`sudo apt-get install bandwidthd`)

# Software

## Compile logtailer

`mvn clean compile assembly:single`

## Compile logarchiver

`mvn clean package spring-boot:repackage`

## init, launch and stop scripts

Use `launch_all.sh` and `stop_all.sh` in your `adtrap` folder.

## Web Console

### DHCP Information / Top Clients

![DHCP Information / Top Clients](/images/dhcp_clients.png?raw=true)

### Top Requests

![Top Requests](/images/request_statistics.png?raw=true)

### Top Advertisers

![Top Advertisers](/images/advertisers_trapped.png?raw=true)

### Statistics

![Statistics](/images/advertisers_statistics.png?raw=true)

### System Monitoring Information

![System Monitoring Information](/images/system_monitor.png?raw=true)

## Database monitoring

Connect to your database with IDE of choice and see queries: 

```
select *
from DNS_LOG_LINES
order by LOG_TIME desc
go

SELECT QUERY_TYPE, count(*) as TOTAL
FROM DNS_LOG_LINES
GROUP BY QUERY_TYPE
ORDER BY 2 DESC
go

SELECT QUERY_DOMAIN, count(*) as TOTAL
FROM DNS_LOG_LINES
GROUP BY QUERY_DOMAIN
ORDER BY 2 DESC
go

SELECT QUERY_TARGET, count(*) as TOTAL
FROM DNS_LOG_LINES
GROUP BY QUERY_TARGET
ORDER BY 2 DESC
go

select QUERY_DOMAIN, count(*) as TOTAL
FROM DNS_LOG_LINES
group by QUERY_DOMAIN
order by 2 desc
go

select QUERY_DOMAIN, count(*) as TOTAL
from DNS_LOG_LINES
where QUERY_TARGET = '?????'
group by QUERY_DOMAIN
order by 2 desc
go

select count(*) as TOTAL
from DNS_LOG_LINES
where QUERY_TARGET = '?????'
go
```

## ToDo

* How to handle HTTPS requests