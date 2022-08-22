# centos7安装

## 下载官方镜像

访问官方网站：https://www.centos.org/download/

点击 [x86_64](http://isoredirect.centos.org/centos/7/isos/x86_64/) 进入镜像源地址

![image-20220819143622358](./images/image-20220819143622358.png)

选择阿里云镜像地址

![image-20220819144126204](./images/image-20220819144126204.png)



选择标准版或者mini版本下载

![image-20220819144247869](./images/image-20220819144247869.png)

## 创建虚拟机

选择自定义，点击下一步

![image-20220819145326980](./images/image-20220819145326980.png)



![image-20220819145420363](./images/image-20220819145420363.png)



选择稍后安装操作系统，点击下一步

![image-20220819145458450](./images/image-20220819145458450.png)



选择centos7 64操作系统

![image-20220819145713445](./images/image-20220819145713445.png)



填写虚拟机名称与存储路径

![image-20220819145805816](./images/image-20220819145805816.png)





![image-20220819145859977](./images/image-20220819145859977.png)



分配虚拟内存

![image-20220819150110170](./images/image-20220819150110170.png)



选择NAT网络



![image-20220819150234457](./images/image-20220819150234457.png)

指定磁盘容量

![image-20220819150344251](./images/image-20220819150344251.png)



自定义硬件，删除掉无用的声卡、打印机等硬件

![image-20220819145145399](./images/image-20220819145145399.png)



## 安装ios镜像

右键虚拟机点击设置，在CD/DVD选项选择使用IOS镜像文件，选择从官方下载下来的ios文件

![image-20220819151001373](./images/image-20220819151001373.png)



点击开启此虚拟机



![image-20220819151431177](C:\Users\12553\AppData\Roaming\Typora\typora-user-images\image-20220819151431177.png)



点击安装，选择英文，点击确定

![image-20220819151542652](C:\Users\12553\AppData\Roaming\Typora\typora-user-images\image-20220819151542652.png)



点击network &hostname，配置网络与主机名称

![image-20220819153356430](./images/image-20220819153356430.png)



填写host名称，打开以太网配置开关，点击配置

![image-20220819152201190](./images/image-20220819152201190.png)



配置静态IP

![image-20220819153009801](./images/image-20220819153009801.png)



点击INSTALLTION DESTINATION 配置安装目标

![image-20220819153518141](./images/image-20220819153518141.png)



直接选择done

![image-20220819153731604](./images/image-20220819153731604.png)





点击 DATE & TIME 配置时区

![image-20220819154059074](./images/image-20220819154059074.png)

选择上海时区



![image-20220819153941453](./images/image-20220819153941453.png)





配置完成后，点击开始安装

![image-20220819154223541](./images/image-20220819154223541.png)



配置root密码，等待安装完成

![image-20220819154506695](./images/image-20220819154506695.png)



安装完成点击reboot登录

![image-20220819155216699](C:\Users\12553\AppData\Roaming\Typora\typora-user-images\image-20220819155216699.png)



# kubeadm高可用安装k8s

## 基本环境配置



​													表1-1  高可用Kubernetes集群规划

| 主机名            | IP地址               | 规格                          | 说明             |
| ----------------- | -------------------- | ----------------------------- | ---------------- |
| k8s-master01 ~ 03 | 192.168.23.101 ~ 103 | (cpu-4 内存-2G 硬盘-20G)/节点 | master节点 * 3   |
| k8s-master-lb     | 192.168.23.100       | 虚拟节点                      | keepalived虚拟IP |
| k8s-node01 ~ 02   | 192.168.23.104 ~ 105 | (cpu-4 内存-4G 硬盘-50G)/节点 | worker节点 * 2   |

**请统一替换这些网段，Pod网段和service和宿主机网段不要重复！！！**



| ***\*配置信息\**** | 备注          |
| ------------------ | ------------- |
| 系统版本           | CentOS 7.9    |
| Docker版本         | 20.10.x       |
| Pod网段            | 172.16.0.0/12 |
| Service网段        | 10.96.0.0/12  |



**注意：宿主机网段、K8s Service网段、Pod网段不能重复**

**VIP（虚拟IP）不要和公司内网IP重复，首先去ping一下，不通才可用。VIP需要和你的主机在同一个局域网内（不是直接用我的VIP）！**

**公有云上搭建VIP是公有云的负载均衡的IP，比如阿里云的内网SLB的地址，腾讯云内网ELB的地址。不需要再搭建keepalived和haproxy**



所有节点配置hosts，修改/etc/hosts如下：

请统一替换这些IP地址！！！

```
[root@k8s-master01 ~]# cat /etc/hosts

192.168.23.101 k8s-master01

192.168.23.102 k8s-master02

192.168.23.103 k8s-master03

192.168.23.100 k8s-master-lb # 如果不是高可用集群，该IP为Master01的IP

192.168.23.104 k8s-node01

192.168.23.105 k8s-node02
```



CentOS 7安装yum源如下：

```
curl -o /etc/yum.repos.d/CentOS-Base.repo https://mirrors.aliyun.com/repo/Centos-7.repo
yum install -y yum-utils device-mapper-persistent-data lvm2
yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
cat <<EOF > /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64/
enabled=1
gpgcheck=0
repo_gpgcheck=0
gpgkey=https://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg https://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg
EOF
sed -i -e '/mirrors.cloud.aliyuncs.com/d' -e '/mirrors.aliyuncs.com/d' /etc/yum.repos.d/CentOS-Base.repo
```



必备工具安装

```
yum install wget jq psmisc vim net-tools telnet yum-utils device-mapper-persistent-data lvm2 git -y
```



所有节点关闭防火墙、selinux、dnsmasq、swap。服务器配置如下：

```
systemctl disable --now firewalld 

#没有dns服务这个命令会报错，不影响
systemctl disable --now dnsmasq
#公有云确认是否需要关闭
systemctl disable --now NetworkManager


setenforce 0
sed -i 's#SELINUX=enforcing#SELINUX=disabled#g' /etc/sysconfig/selinux
sed -i 's#SELINUX=enforcing#SELINUX=disabled#g' /etc/selinux/config

#关闭swap分区
swapoff -a && sysctl -w vm.swappiness=0
sed -ri '/^[^#]*swap/s@^@#@' /etc/fstab
```



 

安装ntpdate

```
rpm -ivh http://mirrors.wlnmp.com/centos/wlnmp-release-centos.noarch.rpm
yum install ntpdate -y
```

 

所有节点同步时间。时间同步配置如下：

```
ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
echo 'Asia/Shanghai' >/etc/timezone
ntpdate time2.aliyun.com
# 加入到crontab (vi /etc/crontab) 
*/5 * * * * /usr/sbin/ntpdate time2.aliyun.com
```



所有节点配置limit：

```
ulimit -SHn 65535
```

vim /etc/security/limits.conf

**# 末尾添加如下内容**

```
* soft nofile 65536
* hard nofile 131072
* soft nproc 65535
* hard nproc 655350
* soft memlock unlimited
* hard memlock unlimited
```



Master01节点免密钥登录其他节点，安装过程中生成配置文件和证书均在Master01上操作，集群管理也在Master01上操作，阿里云或者AWS上需要单独一台kubectl服务器。密钥配置如下：

```
ssh-keygen -t rsa
for i in k8s-master01 k8s-master02 k8s-master03 k8s-node01 k8s-node02;do ssh-copy-id -i .ssh/id_rsa.pub root@$i;done
```





 

所有节点升级系统并重启，此处升级没有升级内核，下节会单独升级内核：

```
yum update -y --exclude=kernel* && reboot #CentOS7需要升级，CentOS8可以按需升级系统
```

## 内核配置

CentOS7 需要升级内核至4.18+，本地升级的版本为4.19

在master01节点下载内核：

```
cd /root
wget http://193.49.22.109/elrepo/kernel/el7/x86_64/RPMS/kernel-ml-devel-4.19.12-1.el7.elrepo.x86_64.rpm
wget http://193.49.22.109/elrepo/kernel/el7/x86_64/RPMS/kernel-ml-4.19.12-1.el7.elrepo.x86_64.rpm
```

从master01节点传到其他节点：

```
for i in k8s-master02 k8s-master03 k8s-node01 k8s-node02;do scp kernel-ml-4.19.12-1.el7.elrepo.x86_64.rpm kernel-ml-devel-4.19.12-1.el7.elrepo.x86_64.rpm $i:/root/ ; done
```

所有节点安装内核

```
cd /root && yum localinstall -y kernel-ml*
```

所有节点更改内核启动顺序

```
grub2-set-default  0 && grub2-mkconfig -o /etc/grub2.cfg

grubby --args="user_namespace.enable=1" --update-kernel="$(grubby --default-kernel)"
```

检查默认内核是不是4.19

```
[root@k8s-master02 ~]# grubby --default-kernel
/boot/vmlinuz-4.19.12-1.el7.elrepo.x86_64
```

所有节点重启，然后检查内核是不是4.19

```
[root@k8s-master02 ~]# uname -a
Linux k8s-master02 4.19.12-1.el7.elrepo.x86_64 #1 SMP Fri Dec 21 11:06:36 EST 2018 x86_64 x86_64 x86_64 GNU/Linux
```

所有节点安装ipvsadm：

```
yum install ipvsadm ipset sysstat conntrack libseccomp -y
```

所有节点配置ipvs模块，**在内核4.19+版本nf_conntrack_ipv4已经改为nf_conntrack， 4.18以下使用nf_conntrack_ipv4**即可：

```
modprobe -- ip_vs
modprobe -- ip_vs_rr
modprobe -- ip_vs_wrr
modprobe -- ip_vs_sh
modprobe -- nf_conntrack
vim /etc/modules-load.d/ipvs.conf 
	# 加入以下内容
ip_vs
ip_vs_lc
ip_vs_wlc
ip_vs_rr
ip_vs_wrr
ip_vs_lblc
ip_vs_lblcr
ip_vs_dh
ip_vs_sh
ip_vs_fo
ip_vs_nq
ip_vs_sed
ip_vs_ftp
ip_vs_sh
nf_conntrack
ip_tables
ip_set
xt_set
ipt_set
ipt_rpfilter
ipt_REJECT
ipip
```

然后执行systemctl enable --now systemd-modules-load.service即可



开启一些k8s集群中必须的内核参数，所有节点配置k8s内核：

```
cat <<EOF > /etc/sysctl.d/k8s.conf
net.ipv4.ip_forward = 1
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
fs.may_detach_mounts = 1
net.ipv4.conf.all.route_localnet = 1
vm.overcommit_memory=1
vm.panic_on_oom=0
fs.inotify.max_user_watches=89100
fs.file-max=52706963
fs.nr_open=52706963
net.netfilter.nf_conntrack_max=2310720

net.ipv4.tcp_keepalive_time = 600
net.ipv4.tcp_keepalive_probes = 3
net.ipv4.tcp_keepalive_intvl =15
net.ipv4.tcp_max_tw_buckets = 36000
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_max_orphans = 327680
net.ipv4.tcp_orphan_retries = 3
net.ipv4.tcp_syncookies = 1
net.ipv4.tcp_max_syn_backlog = 16384
net.ipv4.ip_conntrack_max = 65536
net.ipv4.tcp_max_syn_backlog = 16384
net.ipv4.tcp_timestamps = 0
net.core.somaxconn = 16384
EOF
sysctl --system
```

所有节点配置完内核后，重启服务器，保证重启后内核依旧加载

```
reboot
lsmod | grep --color=auto -e ip_vs -e nf_conntrack
```

## **k8s组件和Runtime安装**

如果安装的版本低于1.24，选择Docker和Containerd均可，高于1.24选择Containerd作为Runtime。

**注意：Runtime安装选择两个小节的其中一个小节即可。**



下载安装所有的源码文件：

```
cd /root/ ; git clone https://github.com/dotbalo/k8s-ha-install.git
```

如果无法下载就下载：https://gitee.com/dukuan/k8s-ha-install.git





### Containerd作为Runtime

所有节点安装docker-ce-20.10：

```
yum install docker-ce-20.10.* docker-ce-cli-20.10.* -y
```

可以无需启动Docker，只需要配置和启动Containerd即可。

首先配置Containerd所需的模块（所有节点）：

```
# cat <<EOF | sudo tee /etc/modules-load.d/containerd.conf
overlay
br_netfilter
EOF
```

所有节点加载模块：

```
# modprobe -- overlay
# modprobe -- br_netfilter
```

所有节点，配置Containerd所需的内核：

```
# cat <<EOF | sudo tee /etc/sysctl.d/99-kubernetes-cri.conf
net.bridge.bridge-nf-call-iptables  = 1
net.ipv4.ip_forward                 = 1
net.bridge.bridge-nf-call-ip6tables = 1
EOF
```

所有节点加载内核：

```
# sysctl --system
```

所有节点配置Containerd的配置文件：

```
# mkdir -p /etc/containerd
# containerd config default | tee /etc/containerd/config.toml
```

所有节点将Containerd的Cgroup改为Systemd：

```
# vim /etc/containerd/config.toml
```

找到***containerd.runtimes.runc.options，添加*SystemdCgroup = true**（**如果已存在直接修改，否则会报错**），如下图所示：

![image-20220822135253640](./images/image-20220822135253640.png)

所有节点将sandbox_image的Pause镜像改成符合自己版本的地址：

registry.cn-hangzhou.aliyuncs.com/google_containers/pause:3.6

![image-20220822135424146](./images/image-20220822135424146.png)



所有节点启动Containerd，并配置开机自启动：

```
# systemctl daemon-reload
# systemctl enable --now containerd
```

所有节点配置crictl客户端连接的运行时位置：

```
# cat > /etc/crictl.yaml <<EOF
runtime-endpoint: unix:///run/containerd/containerd.sock
image-endpoint: unix:///run/containerd/containerd.sock
timeout: 10
debug: false
EOF
```

### Docker作为Runtime(小于1.24)

如果选择Docker作为Runtime，安装步骤较Containerd较为简单，只需要安装并启动即可。

所有节点安装docker-ce 20.10：

```
# yum install docker-ce-20.10.* docker-ce-cli-20.10.* -y
```

由于新版Kubelet建议使用systemd，所以把Docker的CgroupDriver也改成systemd：

```
# mkdir /etc/docker
# cat > /etc/docker/daemon.json <<EOF
{
  "exec-opts": ["native.cgroupdriver=systemd"]
}
EOF
```

所有节点设置开机自启动Docker：

```
# systemctl daemon-reload && systemctl enable --now docker
```

### 安装Kubernetes组件

首先在Master01节点查看最新的Kubernetes版本是多少：

```
# yum list kubeadm.x86_64 --showduplicates | sort -r
```

所有节点安装1.23最新版本kubeadm、kubelet和kubectl：

```
# yum install kubeadm-1.23* kubelet-1.23* kubectl-1.23* -y
```

如果选择的是Containerd作为的Runtime，需要更改Kubelet的配置使用Containerd作为Runtime：

```
# cat >/etc/sysconfig/kubelet<<EOF
KUBELET_KUBEADM_ARGS="--container-runtime=remote --runtime-request-timeout=15m --container-runtime-endpoint=unix:///run/containerd/containerd.sock"
EOF
```

**注意：如果不是采用Containerd作为的Runtime，请不要执行上述命令。**

所有节点设置Kubelet开机自启动（由于还未初始化，没有kubelet的配置文件，此时kubelet无法启动，无需管理）：

```
# systemctl daemon-reload
# systemctl enable --now kubelet
```

此时kubelet是起不来的，日志会有报错不影响！



## 高可用组件安装

**（注意：如果不是高可用集群，haproxy和keepalived无需安装）**

**公有云要用公有云自带的负载均衡，比如阿里云的SLB，腾讯云的ELB，用来替代haproxy和keepalived，因为公有云大部分都是不支持keepalived的，另外如果用阿里云的话，kubectl控制端不能放在master节点，推荐使用腾讯云，因为阿里云的slb有回环的问题，也就是slb代理的服务器不能反向访问SLB，但是腾讯云修复了这个问题。**



所有Master节点通过yum安装HAProxy和KeepAlived：

```
yum install keepalived haproxy -y
```

所有Master节点配置HAProxy（详细配置参考HAProxy文档，所有Master节点的HAProxy配置相同）：

```
[root@k8s-master01 etc]# mkdir /etc/haproxy
[root@k8s-master01 etc]# vim /etc/haproxy/haproxy.cfg 
global
  maxconn  2000
  ulimit-n  16384
  log  127.0.0.1 local0 err
  stats timeout 30s

defaults
  log global
  mode  http
  option  httplog
  timeout connect 5000
  timeout client  50000
  timeout server  50000
  timeout http-request 15s
  timeout http-keep-alive 15s

frontend monitor-in
  bind *:33305
  mode http
  option httplog
  monitor-uri /monitor

frontend k8s-master
  bind 0.0.0.0:16443
  bind 127.0.0.1:16443
  mode tcp
  option tcplog
  tcp-request inspect-delay 5s
  default_backend k8s-master

backend k8s-master
  mode tcp
  option tcplog
  option tcp-check
  balance roundrobin
  default-server inter 10s downinter 5s rise 2 fall 2 slowstart 60s maxconn 250 maxqueue 256 weight 100
  server k8s-master01	192.168.23.101:6443  check
  server k8s-master02	192.168.23.102:6443  check
  server k8s-master03	192.168.23.103:6443  check
```

所有Master节点配置KeepAlived，配置不一样，注意区分，**注意每个节点的IP和网卡（interface参数）**

 Master01节点的配置：

```
[root@k8s-master01 etc]# mkdir /etc/keepalived

[root@k8s-master01 ~]# vim /etc/keepalived/keepalived.conf 
! Configuration File for keepalived
global_defs {
    router_id LVS_DEVEL
script_user root
    enable_script_security
}
vrrp_script chk_apiserver {
    script "/etc/keepalived/check_apiserver.sh"
    interval 5
    weight -5
    fall 2  
rise 1
}
vrrp_instance VI_1 {
    state MASTER
    interface ens33
    mcast_src_ip 192.168.23.101
    virtual_router_id 51
    priority 101
    advert_int 2
    authentication {
        auth_type PASS
        auth_pass K8SHA_KA_AUTH
    }
    virtual_ipaddress {
        192.168.23.100
    }
    track_script {
       chk_apiserver
    }
}
```

Master02节点的配置：

```
! Configuration File for keepalived
global_defs {
    router_id LVS_DEVEL
script_user root
    enable_script_security
}
vrrp_script chk_apiserver {
    script "/etc/keepalived/check_apiserver.sh"
   interval 5
    weight -5
    fall 2  
rise 1
}
vrrp_instance VI_1 {
    state BACKUP
    interface ens33
    mcast_src_ip 192.168.23.102
    virtual_router_id 51
    priority 100
    advert_int 2
    authentication {
        auth_type PASS
        auth_pass K8SHA_KA_AUTH
    }
    virtual_ipaddress {
        192.168.23.100
    }
    track_script {
       chk_apiserver
    }
}
```

Master03节点的配置：

```
! Configuration File for keepalived
global_defs {
    router_id LVS_DEVEL
script_user root
    enable_script_security
}
vrrp_script chk_apiserver {
    script "/etc/keepalived/check_apiserver.sh"
 interval 5
    weight -5
    fall 2  
rise 1
}
vrrp_instance VI_1 {
    state BACKUP
    interface ens33
    mcast_src_ip 192.168.23.103
    virtual_router_id 51
    priority 100
    advert_int 2
    authentication {
        auth_type PASS
        auth_pass K8SHA_KA_AUTH
    }
    virtual_ipaddress {
        192.168.23.100
    }
    track_script {
       chk_apiserver
    }
}
```

所有master节点配置KeepAlived健康检查文件：

```
[root@k8s-master01 keepalived]# cat /etc/keepalived/check_apiserver.sh 
#!/bin/bash

err=0
for k in $(seq 1 3)
do
    check_code=$(pgrep haproxy)
    if [[ $check_code == "" ]]; then
        err=$(expr $err + 1)
        sleep 1
        continue
    else
        err=0
        break
    fi
done

if [[ $err != "0" ]]; then
    echo "systemctl stop keepalived"
    /usr/bin/systemctl stop keepalived
    exit 1
else
    exit 0
fi
```

添加脚本可执行权限

```
chmod +x /etc/keepalived/check_apiserver.sh
```

启动haproxy和keepalived

```
[root@k8s-master01 keepalived]# systemctl daemon-reload
[root@k8s-master01 keepalived]# systemctl enable --now haproxy
[root@k8s-master01 keepalived]# systemctl enable --now keepalived
```

**重要：如果安装了keepalived和haproxy，需要测试keepalived是否是正常的**

```
#测试VIP
[root@k8s-master01 ~]# ping 192.168.23.100 -c 4
PING 192.168.23.100 (192.168.23.100) 56(84) bytes of data.
64 bytes from 192.168.23.100: icmp_seq=1 ttl=64 time=0.464 ms
64 bytes from 192.168.23.100: icmp_seq=2 ttl=64 time=0.063 ms
64 bytes from 192.168.23.100: icmp_seq=3 ttl=64 time=0.062 ms
64 bytes from 192.168.23.100: icmp_seq=4 ttl=64 time=0.063 ms

--- 192.168.23.100 ping statistics ---
4 packets transmitted, 4 received, 0% packet loss, time 3106ms
rtt min/avg/max/mdev = 0.062/0.163/0.464/0.173 ms

[root@k8s-master01 ~]# telnet 192.168.23.100 16443
Trying 192.168.23.100...
Connected to 192.168.23.100.
Escape character is '^]'.
Connection closed by foreign host.
```

如果ping不通且telnet没有出现 ] ，则认为VIP不可以，不可在继续往下执行，需要排查keepalived的问题，比如防火墙和selinux，haproxy和keepalived的状态，监听端口等

所有节点查看防火墙状态必须为disable和inactive：systemctl status firewalld

所有节点查看selinux状态，必须为disable：getenforce

master节点查看haproxy和keepalived状态：systemctl status keepalived haproxy

master节点查看监听端口：netstat -lntp



## 集群初始化

官方初始化文档：

https://kubernetes.io/docs/setup/production-environment/tools/kubeadm/high-availability/

以下操作只在master01节点执行

Master01节点创建kubeadm-config.yaml配置文件如下：

Master01：（**注意，如果不是高可用集群，192.168.23.100:16443改为master01的地址，16443改为apiserver的端口，默认是6443，注意更改kubernetesVersion的值和自己服务器kubeadm的版本一致：kubeadm version**）



**以下文件内容，宿主机网段、podSubnet网段、serviceSubnet网段不能重复，具体看课程资料的【安装前必看】集群安装网段划分**

**以下操作在master01：**

```
[root@k8s-master01 ~]vim kubeadm-config.yaml
apiVersion: kubeadm.k8s.io/v1beta2
bootstrapTokens:
- groups:
  - system:bootstrappers:kubeadm:default-node-token
  token: 7t2weq.bjbawausm0jaxury
  ttl: 24h0m0s
  usages:
  - signing
  - authentication
kind: InitConfiguration
localAPIEndpoint:
  advertiseAddress: 192.168.23.101
  bindPort: 6443
nodeRegistration:
  # criSocket: /var/run/dockershim.sock  # 如果是Docker作为Runtime配置此项
  criSocket: /run/containerd/containerd.sock # 如果是Containerd作为Runtime配置此项
  name: k8s-master01
  taints:
  - effect: NoSchedule
    key: node-role.kubernetes.io/master
---
apiServer:
  certSANs:
  - 192.168.23.100
  timeoutForControlPlane: 4m0s
apiVersion: kubeadm.k8s.io/v1beta2
certificatesDir: /etc/kubernetes/pki
clusterName: kubernetes
controlPlaneEndpoint: 192.168.23.100:16443
controllerManager: {}
dns:
  type: CoreDNS
etcd:
  local:
    dataDir: /var/lib/etcd
imageRepository: registry.cn-hangzhou.aliyuncs.com/google_containers
kind: ClusterConfiguration
kubernetesVersion: v1.23.10 # 更改此处的版本号和kubeadm version一致
networking:
  dnsDomain: cluster.local
  podSubnet: 172.16.0.0/12
  serviceSubnet: 10.96.0.0/12
scheduler: {}
```

更新kubeadm文件

```
kubeadm config migrate --old-config kubeadm-config.yaml --new-config new.yaml
```

将new.yaml文件复制到其他master节点：

```
for i in k8s-master02 k8s-master03; do scp new.yaml $i:/root/; done
```

之后所有Master节点提前下载镜像，可以节省初始化时间（其他节点不需要更改任何配置，包括IP地址也不需要更改）：

```
kubeadm config images pull --config /root/new.yaml 
```

***所有节点***设置开机自启动kubelet

```
#（如果启动失败无需管理，初始化成功以后即可启动）
systemctl enable --now kubelet
```

Master01节点初始化，初始化以后会在/etc/kubernetes目录下生成对应的证书和配置文件，之后其他Master节点加入Master01即可：

```
kubeadm init --config /root/new.yaml  --upload-certs
```

**如果初始化失败，重置后再次初始化，命令如下（没有失败不要执行）：**

```
kubeadm reset -f ; ipvsadm --clear  ; rm -rf ~/.kube
```

初始化成功以后，会产生Token值，用于其他节点加入时使用，因此要记录下初始化成功生成的token值（令牌值）：

```
Your Kubernetes control-plane has initialized successfully!

To start using your cluster, you need to run the following as a regular user:

  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config

Alternatively, if you are the root user, you can run:

  export KUBECONFIG=/etc/kubernetes/admin.conf

You should now deploy a pod network to the cluster.
Run "kubectl apply -f [podnetwork].yaml" with one of the options listed at:
  https://kubernetes.io/docs/concepts/cluster-administration/addons/

You can now join any number of the control-plane node running the following command on each as root:

  kubeadm join 192.168.23.100:16443 --token 7t2weq.bjbawausm0jaxury \
	--discovery-token-ca-cert-hash sha256:df72788de04bbc2e8fca70becb8a9e8503a962b5d7cd9b1842a0c39930d08c94 \
	--control-plane --certificate-key c595f7f4a7a3beb0d5bdb75d9e4eff0a60b977447e76c1d6885e82c3aa43c94c

Please note that the certificate-key gives access to cluster sensitive data, keep it secret!
As a safeguard, uploaded-certs will be deleted in two hours; If necessary, you can use
"kubeadm init phase upload-certs --upload-certs" to reload certs afterward.

Then you can join any number of worker nodes by running the following on each as root:

kubeadm join 192.168.23.100:16443 --token 7t2weq.bjbawausm0jaxury \
	--discovery-token-ca-cert-hash sha256:df72788de04bbc2e8fca70becb8a9e8503a962b5d7cd9b1842a0c39930d08c94
```

Master01节点配置环境变量，用于访问Kubernetes集群：

```
cat <<EOF >> /root/.bashrc
export KUBECONFIG=/etc/kubernetes/admin.conf
EOF
source /root/.bashrc
```

查看节点状态：

```
[root@k8s-master01 ~]# kubectl get node

```

采用初始化安装方式，所有的系统组件均以容器的方式运行并且在kube-system命名空间内，此时可以查看Pod状态：

```
[root@k8s-master01 ~]# kubectl get po -n kube-system
```

![image-20220822143009058](./images/image-20220822143009058.png)



## 高可用Master

**注意：以下步骤是上述init命令产生的Token过期了才需要执行以下步骤，如果没有过期不需要执行，直接join即可**

```
#Token过期后生成新的token
kubeadm token create --print-join-command

#Master需要生成--certificate-key
kubeadm init phase upload-certs  --upload-certs
```

 

其他**master**加入集群，master02和master03分别执行

```
kubeadm join 192.168.23.100:16443 --token 7t2weq.bjbawausm0jaxury \
	--discovery-token-ca-cert-hash sha256:df72788de04bbc2e8fca70becb8a9e8503a962b5d7cd9b1842a0c39930d08c94 \
	--control-plane --certificate-key c595f7f4a7a3beb0d5bdb75d9e4eff0a60b977447e76c1d6885e82c3aa43c94c
```

查看当前状态

```
[root@k8s-master01 ~]# kubectl get node
```

![image-20220822143557724](./images/image-20220822143557724.png)



## Node节点的配置

Node节点上主要部署公司的一些业务应用，生产环境中不建议Master节点部署系统组件之外的其他Pod，测试环境可以允许Master节点部署Pod以节省系统资源。

```
kubeadm join 192.168.23.100:16443 --token 7t2weq.bjbawausm0jaxury \
	--discovery-token-ca-cert-hash sha256:df72788de04bbc2e8fca70becb8a9e8503a962b5d7cd9b1842a0c39930d08c94
```

所有节点初始化完成后，查看集群状态

```
[root@k8s-master01 ~]# kubectl get node
```

![image-20220822143840119](./images/image-20220822143840119.png)



## 网络插件安装

Calico网络插件支持网络策略配置，选用Calico组件作为网络插件

### Calico组件的安装

```
cd /root/k8s-ha-install && git checkout manual-installation-v1.23.x && cd calico/
```

修改Pod网段：

```
POD_SUBNET=`cat /etc/kubernetes/manifests/kube-controller-manager.yaml | grep cluster-cidr= | awk -F= '{print $NF}'`

sed -i "s#POD_CIDR#${POD_SUBNET}#g" calico.yaml
kubectl apply -f calico.yaml
```

查看容器状态：

```
[root@k8s-master01 ~]# kubectl get po -n kube-system
```

![image-20220822144719890](./images/image-20220822144719890.png)

查看节点状态：

```
[root@k8s-master01 ~]# kubectl get node -O wide
```

![image-20220822144833837](./images/image-20220822144833837.png)



## Metrics部署

在新版的Kubernetes中系统资源的采集均使用Metrics-server，可以通过Metrics采集节点和Pod的内存、磁盘、CPU和网络的使用率。



将Master01节点的front-proxy-ca.crt复制到所有Node节点：

```
scp /etc/kubernetes/pki/front-proxy-ca.crt k8s-node01:/etc/kubernetes/pki/front-proxy-ca.crt
scp /etc/kubernetes/pki/front-proxy-ca.crt k8s-node(其他节点自行拷贝):/etc/kubernetes/pki/front-proxy-ca.crt
```

安装metrics server

```
cd /root/k8s-ha-install/kubeadm-metrics-server

# kubectl  create -f comp.yaml 
serviceaccount/metrics-server created
clusterrole.rbac.authorization.k8s.io/system:aggregated-metrics-reader created
clusterrole.rbac.authorization.k8s.io/system:metrics-server created
rolebinding.rbac.authorization.k8s.io/metrics-server-auth-reader created
clusterrolebinding.rbac.authorization.k8s.io/metrics-server:system:auth-delegator created
clusterrolebinding.rbac.authorization.k8s.io/system:metrics-server created
service/metrics-server created
deployment.apps/metrics-server created
apiservice.apiregistration.k8s.io/v1beta1.metrics.k8s.io created
```

查看状态

```
kubectl get po -n kube-system -l k8s-app=metrics-server
```

![image-20220822145326162](./images/image-20220822145326162.png)



变成1/1   Running后

查看节点使用率

```
[root@k8s-master01 ~]# kubectl top node
```

![image-20220822145539523](./images/image-20220822145539523.png)

查看容器使用率

```
[root@k8s-master01 ~]# kubectl top po -A
```

![image-20220822145649151](./images/image-20220822145649151.png)



## Dashboard部署

Dashboard用于展示集群中的各类资源，同时也可以通过Dashboard实时查看Pod的日志和在容器中执行一些命令等。

### 安装指定版本

```
[root@k8s-master01 ~]# cd /root/k8s-ha-install/dashboard/

[root@k8s-master01 dashboard]# kubectl  create -f .
serviceaccount/admin-user created
clusterrolebinding.rbac.authorization.k8s.io/admin-user created
namespace/kubernetes-dashboard created
serviceaccount/kubernetes-dashboard created
service/kubernetes-dashboard created
secret/kubernetes-dashboard-certs created
secret/kubernetes-dashboard-csrf created
secret/kubernetes-dashboard-key-holder created
configmap/kubernetes-dashboard-settings created
role.rbac.authorization.k8s.io/kubernetes-dashboard created
clusterrole.rbac.authorization.k8s.io/kubernetes-dashboard created
rolebinding.rbac.authorization.k8s.io/kubernetes-dashboard created
clusterrolebinding.rbac.authorization.k8s.io/kubernetes-dashboard created
deployment.apps/kubernetes-dashboard created
service/dashboard-metrics-scraper created
deployment.apps/dashboard-metrics-scraper created
```

### 安装最新版本

官方GitHub地址：https://github.com/kubernetes/dashboard

可以在官方dashboard查看到最新版dashboard

![image-20220822150139041](./images/image-20220822150139041.png)



```
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.3/aio/deploy/recommended.yaml
```

以具体版本号为准

```
[root@k8s-master01 ~]# vim admin.yaml

apiVersion: v1
kind: ServiceAccount
metadata:
  name: admin-user
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding 
metadata: 
  name: admin-user
  annotations:
    rbac.authorization.kubernetes.io/autoupdate: "true"
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: admin-user
  namespace: kube-system
```



```
kubectl apply -f admin.yaml -n kube-system
```



### 登录dashboard

在谷歌浏览器（Chrome）启动文件中加入启动参数，用于解决无法访问Dashboard的问题：

```
--test-type --ignore-certificate-errors
```

![image-20220822150503399](./images/image-20220822150503399.png)



更改dashboard的svc为NodePort：

```
kubectl edit svc kubernetes-dashboard -n kubernetes-dashboard
```

![image-20220822150610775](./images/image-20220822150610775.png)

将ClusterIP更改为NodePort（如果已经为NodePort忽略此步骤）：

查看端口号：

```
kubectl get svc kubernetes-dashboard -n kubernetes-dashboard
```

![image-20220822150736140](./images/image-20220822150736140.png)



根据自己的实例端口号，通过任意安装了kube-proxy的宿主机的IP+端口即可访问到dashboard：

访问Dashboard：[https://192.168.23.101:18282（请更改18282为自己的端口）](https://10.103.236.201:18282（请更改18282为自己的端口）)，选择登录方式为令牌（即token方式）

![image-20220822150922234](./images/image-20220822150922234.png)

查看token值：

```
[root@k8s-master01 1.1.1]# kubectl -n kube-system describe secret $(kubectl -n kube-system get secret | grep admin-user | awk '{print $1}')
Name:         admin-user-token-r4vcp
Namespace:    kube-system
Labels:       <none>
Annotations:  kubernetes.io/service-account.name: admin-user
              kubernetes.io/service-account.uid: 2112796c-1c9e-11e9-91ab-000c298bf023

Type:  kubernetes.io/service-account-token

Data
====
ca.crt:     1025 bytes
namespace:  11 bytes
token:      eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi11c2VyLXRva2VuLXI0dmNwIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFkbWluLXVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiIyMTEyNzk2Yy0xYzllLTExZTktOTFhYi0wMDBjMjk4YmYwMjMiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YWRtaW4tdXNlciJ9.bWYmwgRb-90ydQmyjkbjJjFt8CdO8u6zxVZh-19rdlL_T-n35nKyQIN7hCtNAt46u6gfJ5XXefC9HsGNBHtvo_Ve6oF7EXhU772aLAbXWkU1xOwQTQynixaypbRIas_kiO2MHHxXfeeL_yYZRrgtatsDBxcBRg-nUQv4TahzaGSyK42E_4YGpLa3X3Jc4t1z0SQXge7lrwlj8ysmqgO4ndlFjwPfvg0eoYqu9Qsc5Q7tazzFf9mVKMmcS1ppPutdyqNYWL62P1prw_wclP0TezW1CsypjWSVT4AuJU8YmH8nTNR1EXn8mJURLSjINv6YbZpnhBIPgUGk1JYVLcn47w
```

将token值输入到令牌后，单击登录即可访问Dashboard

![image-20220822151126895](./images/image-20220822151126895.png)





## 

## 一些必须的配置更改-【必看】

将Kube-proxy改为ipvs模式，因为在初始化集群的时候注释了ipvs配置，所以需要自行修改一下：

在master01节点执行

```
kubectl edit cm kube-proxy -n kube-system
mode: ipvs
```

更新Kube-Proxy的Pod：

```
kubectl patch daemonset kube-proxy -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"date\":\"`date +'%s'`\"}}}}}" -n kube-system
```

验证Kube-Proxy模式：

```
[root@k8s-master01 1.1.1]# curl 127.0.0.1:10249/proxyMode
ipvs
```





