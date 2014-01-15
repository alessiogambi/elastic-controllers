grant usage on Controller.* to controller@'localhost' identified by 'controller';
grant all on Controller.* to controller@'localhost' identified by 'controller';

grant usage on Controller.* to controller@'%' identified by 'controller';
grant all on Controller.* to controller@'%' identified by 'controller';

flush privileges;
