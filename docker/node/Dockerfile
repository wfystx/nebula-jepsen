FROM vesoft/nebula-storaged:jepsen-v2

ADD run.sh /
ADD start-stop-daemon /usr/bin/

RUN yum install -y openssh* sudo net-tools wget curl vim iptables initscripts make gcc gcc-c++ ncurses-devel

COPY sshd_config /etc/ssh/sshd_config

RUN service sshd restart

ENTRYPOINT ["/run.sh"]
