#!/bin/bash

#
# We assume that you already have compiled the based project via: mvn clean compile package appassembler:assemble
# And also capied the folders target/appassembler/bin and target/appassembler/repo into /opt/doodlecontroller
#
COMPONENT_NAME="doodlecontroller"
COMPONENT_PATH="/opt/$COMPONENT_NAME/"
LOG_FOLDER="/var/log/$COMPONENT_NAME/"

write_log()
{
  if [ ! -e "$LOG_FILE" ] ; then setup_log ; fi
  echo "* $*" | tee -a "$LOG_FILE"
}

setup_log()
{
  if [ ! -e "$LOG_FOLDER" ]
  then
    mkdir "$LOG_FOLDER"
  fi
  LOG_FILE="$LOG_FOLDER/$COMPONENT_NAME.log"
  touch $LOG_FILE
}

#
# With need to perform all the configurations step that this component needs:
# 1 - Update the spring file with the database data if there is an option for that otherwise assume localhost by default
#
configure()
{
        write_log "Configure $COMPONENT_NAME"

	sudo service mysql start

	CONFIG_FILE=$COMPONENT_PATH"cloud.properties"
	touch $CONFIG_FILE

	# Extract from the ENV all the properties that match this component name space
        local pattern=$COMPONENT_NAME"_env_"
	for customp in `env | grep "^$pattern" | sed "s/^$pattern//"`
	do
		write_log "Read $customp from user data"

		local custompname=`echo $customp | cut -d "=" -f 1`
		local custompvalue=`echo $customp | cut -d "=" -f 2`

		# Process the property
		case "$custompname" in
		'ossecretkey')
			echo "ch.usi.cloud.controller.eucalyptus.secretKey=$custompvalue" >> $CONFIG_FILE
		;;
		'osaccesskey')
			echo "ch.usi.cloud.controller.eucalyptus.accessKey=$custompvalue" >> $CONFIG_FILE
		;;
		'osccport')
			echo "ch.usi.cloud.controller.eucalyptus.ccPort=$custompvalue" >> $CONFIG_FILE
		;;
		'osccaddress')
			echo "ch.usi.cloud.controller.eucalyptus.ccAddress=$custompvalue" >> $CONFIG_FILE
		;;
        'jarurl')
                        JAR=$COMPONENT_PATH"user.jar"
                        curl $custompvalue > $JAR
                        local result=$?
                        if [ "$result" == "0" ]; then
                                write_log "User jar dowloaded to $JAR"
                        else
                                JAR=
                        fi
        ;;
		*)
			write_log "$customp is an invalid env option. Skipped"
		;;
		esac
	done
}


#
# The component may need startup options or additional command line arguments to start
#
buildStartupOption(){

	local controller="rules"
	local monitoringip="127.0.0.1"
	
	STARTUP_OPTIONS="-Dlog4j.configuration=file:"$COMPONENT_PATH"conf/log4j.properties "
    STARTUP_OPTIONS="$STARTUP_OPTIONS -Dat.ac.tuwien.dsg.cloud.configuration=$CONFIG_FILE "

    # Default values
    write_log "Default Startup options:"
    write_log "$STARTUP_OPTIONS"

        # Extract from the ENV all the properties that match this component name space on startup
        local pattern=$COMPONENT_NAME"_startup_"
        for customp in `env | grep "^$pattern" | sed "s/^$pattern//"`
        do
                write_log "Read $customp from user data"

                local custompname=`echo $customp | cut -d "=" -f 1`
		# This will fail when custompvalue constains the '=' char therefore we use the sed based approach
                # local custompvalue=`echo $customp | cut -d "=" -f 2`
                local custompvalue=`echo $customp | sed -e "s/$custompname//" -e 's/=//'`

		case "$custompname" in
                'servicefqn')
			local customerName=`echo "$custompvalue" | cut -d "." -f 3`
			local serviceName=`echo "$custompvalue" | cut -d "." -f 5`
			local organizationName=`echo "$custompvalue" | cut -d "." -f 1`
                ;;
		'deployid')
			local deployid=$custompvalue
		;;
		'controller')
			local controller=$custompvalue
		;;
		'manifesturl')
			local manifesturl=$custompvalue
		;;
		'monitoringip')
			local monitoringip=$custompvalue
		;;
		*)
			write_log "$customp is an invalid startup option. Skipped"
		;;
		esac
	done

	# Must be 6 !
	ARGS="$organizationName $customerName $serviceName $deployid $manifesturl $controller"
	STARTUP_OPTIONS="$STARTUP_OPTIONS -Dch.usi.cloud.controller.doodleservice.monitoring.db.host=$monitoringip"

	write_log "Inject RUNTIME Values"
	cd $COMPONENT_PATH
	cp bin/$COMPONENT_NAME.original bin/$COMPONENT_NAME
        
	if [ ! -z "$STARTUP_OPTIONS" ] ; then
        sed -i bin/$COMPONENT_NAME -e "s|@EXTRA_JVM_ARGUMENTS|EXTRA_JVM_ARGUMENTS=\"$STARTUP_OPTIONS\"|"
	else
        write_log "No additional startup options remove @EXTRA_JVM_ARGUMENTS placeholder"
        sed -i bin/$COMPONENT_NAME -e '/@EXTRA_JVM_ARGUMENTS/d'
	fi
                        
	if [ ! -z "$JAR" ] ; then
        sed -i bin/$COMPONENT_NAME -e "s|@CLASSPATH|CLASSPATH=\"\$CLASSPATH:$JAR\"|"
	else
        write_log "No additional JARS remove the @CLASSPATH placeholder"
        sed -i bin/$COMPONENT_NAME -e '/@CLASSPATH/d'
	fi
	
	cd -
}

start() {
	write_log "  Starting $COMPONENT_NAME"

	configure

	buildStartupOption

	local oldpath="$PWD"
	cd "$COMPONENT_PATH"
	cat > start-in-screen.sh <<!
#!/bin/bash

echo "Starting $COMPONENT_NAME"

# Use the scripts generated by the appassembler maven plugin
sh bin/$COMPONENT_NAME $ARGS 2>&1 | tee -a "$LOG_FOLDER/$COMPONENT_NAME.out"

!
                chmod +x start-in-screen.sh

                 # Start the command inside a screen. Needs to abs path to the file
                screen -S $COMPONENT_NAME -d -m $COMPONENT_PATH""start-in-screen.sh

                # This is an heuristic...
                sleep 5
                if [ `screen -ls "$COMPONENT_NAME" | wc -l` -lt 4 ]
		then
			RETVAL=1
			write_log "Error in starting $COMPONENT_NAME"
		else
		        write_log "  Started"
		fi
	cd "$oldpath"
}

stop()
{
	write_log "  Stopping $COMPONENT_NAME"

        local oldpath="$PWD"
	    cd "$COMPONENT_PATH"
			# ps aux | grep java | grep -v jetty | grep doodlecontroller | awk '{print $2}' | xargs kill -15
			screen -S $COMPONENT_NAME -X quit
        cd "$oldpath"

	write_log "  Stopped"
}

deregister()
{
    echo "Usage: DEPLOY|UNDEPLOY organizationName customerName serviceName manifestURL"

	# Try to undeploy all the VMs that it allocates
        write_log "deregister $COMPONENT_NAME"

	local manifestURL="file://"$COMPONENT_PATH"manifest.xml"

        local pattern=$COMPONENT_NAME"_register_"
        for customp in `env | grep "^$pattern" | sed "s/^$pattern//"`
        do
                write_log "Read $customp from user data"

                local custompname=`echo $customp | cut -d "=" -f 1`
                # This will fail when custompvalue constains the '=' char therefore we use the sed based approach
                # local custompvalue=`echo $customp | cut -d "=" -f 2`
                local custompvalue=`echo $customp | sed -e "s/$custompname//" -e 's/=//'`

                case "$custompname" in
                *)
                        write_log "$customp is an invalid deregistration option. Skipped"
                ;;
                esac
        done
}

clean()
{
        write_log "Clean logs for $COMPONENT_NAME"
        rm -v $LOG_FOLDER""$COMPONENT_NAME.*
        rm -v $LOG_FOLDER"".*
}

#######################################################################
# Here the main begins
#######################################################################

write_log "Export the environment"
. /tmp/userdata

case "$1" in
start)
  start
;;
stop)
  stop
;;
restart)
  stop
  start
;;
deregister)
  deregister
;;
clean)
  clean
*)
echo "Wrong input. Usage: $0 {start|stop|restart|deregister}"
RETVAL=1
;;
esac

exit $RETVAL

