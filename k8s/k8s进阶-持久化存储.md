## Volumes

官方文档 https://kubernetes.io/docs/concepts/storage/volumes/

Volume是Pod中能够被多个容器访问的共享目录，它被定义在Pod上，然后被一个Pod里的多个容器挂载到具体的文件目录下，kubernetes通过Volume实现同一个Pod中不同容器之间的数据共享以及数据的持久化存储。Volume的生命容器不与Pod中单个容器的生命周期相关，当容器终止或者重启时，Volume中的数据也不会丢失。

kubernetes的Volume支持多种类型，比较常见的有下面几个：

- 简单存储：EmptyDir、HostPath、NFS
- 高级存储：PV、PVC
- 配置存储：ConfigMap、Secret



一些需要持久化数据的程序才会用到 Volumes，或者一些需要共享数据的容器需要 volumes。

​     Redis-Cluster：nodes.conf

​     日志收集的需求：需要在应用程序的容器里面加一个 sidecar，这个容器是一个收集日志的容器，比如 filebeat，它通过 volumes 共享           

​                                   应用程序的日志文件目录。



### 背景

Docker 也有卷的概念，但是在 Docker 中卷只是磁盘上或另一个 Container 中的目录，其生命周期不受管理。虽然目前 Docker 已经提

供了卷驱动程序，但是功能非常有限，例如从 Docker1.7 版本开始，每个 Container 只允许一个卷驱动程序，并且无法将参数传递卷。



另一方面，Kubernetes 卷具有明确的生命周期，与使用它的 Pod 相同。因此，在 Kubernetes中的卷可以比 Pod 中运行的任何

Container 都长，并且可以在 Container 重启或者销毁之后保留数据。Kubernetes 支持多种类型的卷，Pod 可以同时使用任意数量卷。



从本质上讲，卷只是一个目录，可能包含一些数据，Pod 中的容器可以访问它。要使用卷 Pod需要通过.spec.volumes 字段指定为 Pod 

提供的卷，以及使用.spec.containers.volumeMounts 字段指定卷挂载的目录。从容器中的进程可以看到由 Docker 镜像和卷组成的文件

系统视图，卷无法挂载其他卷或具有到其他卷的硬链接，Pod 中的每个 Container 必须独立指定每个卷的挂载位置。



###  emptyDir

EmptyDir是最基础的Volume类型，一个EmptyDir就是Host上的一个空目录。

EmptyDir是在Pod被分配到Node时创建的，它的初始内容为空，并且无须指定宿主机上对应的目录文件，因为kubernetes会自动分配一个目录，当Pod销毁时， EmptyDir中的数据也会被永久删除。 EmptyDir用途如下：

- 临时空间，例如用于某些应用程序运行时所需的临时目录，且无须永久保留
- 一个容器需要从另一个容器中获取数据的目录（多容器共享目录）



```yaml
apiVersion: v1
kind: Pod
metadata:
  name: volume-emptydir
spec:
  containers:
  - name: nginx
    image: nginx:1.17.1
    ports:
    - containerPort: 80
    volumeMounts:  # 将logs-volume挂在到nginx容器中，对应的目录为 /var/log/nginx
    - name: logs-volume
      mountPath: /var/log/nginx
  - name: busybox
    image: busybox:1.30
    command: ["/bin/sh","-c","tail -f /logs/access.log"] # 初始命令，动态读取指定文件中内容
    volumeMounts:  # 将logs-volume 挂在到busybox容器中，对应的目录为 /logs
    - name: logs-volume
      mountPath: /logs
  volumes: # 声明volume， name为logs-volume，类型为emptyDir
  - name: logs-volume
    emptyDir: {}
```



### hostPath

HostPath就是将Node主机中一个实际目录挂在到Pod中，以供容器使用，这样的设计就可以保证Pod销毁了，但是数据依据可以存在于Node主机上。

创建一个volume-hostpath.yaml：

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: volume-hostpath
spec:
  containers:
  - name: nginx
    image: nginx:1.17.1
    ports:
    - containerPort: 80
    volumeMounts:
    - name: logs-volume
      mountPath: /var/log/nginx
  - name: busybox
    image: busybox:1.30
    command: ["/bin/sh","-c","tail -f /logs/access.log"]
    volumeMounts:
    - name: logs-volume
      mountPath: /logs
  volumes:
  - name: logs-volume
    hostPath: 
      path: /root/logs
      type: DirectoryOrCreate  # 目录存在就使用，不存在就先创建后使用
```

hostPath 卷常用的 type（类型）如下：

```
type 为空字符串：默认选项，意味着挂载 hostPath 卷之前不会执行任何检查。
DirectoryOrCreate：目录存在就使用，不存在就先创建一个权限为0755 的空目录，和 Kubelet 具有相同的组和权限。
Directory：目录必须存在。
FileOrCreate：文件存在就使用，不存在就根据需要创建一个空文件，权限设置为 0644，和 Kubelet 具有相同的组和所有权。
File：文件必须存在 。
Socket： unix套接字必须存在。
CharDevice：字符设备必须存在。
BlockDevice：块设备必须存在。
```



```shell
[root@k8s-master01 ~]# kubectl create -f volume-hostpath.yaml
pod/volume-hostpath created

[root@k8s-master01 ~]# kubectl get pods volume-hostpath  -o wide
NAME                  READY   STATUS    RESTARTS   AGE   IP             NODE   
pod-volume-hostpath   2/2     Running   0          16s   10.42.2.10     k8s-node01

#访问nginx
[root@k8s-master01 ~]# curl 10.42.2.10

[root@k8s-master01 ~]# kubectl logs -f volume-hostpath -c busybox

# 接下来就可以去host的/root/logs目录下查看存储的文件了
###  注意: 下面的操作需要到Pod所在的节点运行（案例中是 k8s-node01 ）
[root@k8s-node01 ~]# ls /root/logs/
access.log  error.log
```



### NFS

HostPath可以解决数据持久化的问题，但是一旦Node节点故障了，Pod如果转移到了别的节点，又会出现问题了，此时需要准备单独的网络存储系统，比较常用的用NFS、CIFS。

NFS是一个网络文件存储系统，可以搭建一台NFS服务器，然后将Pod中的存储直接连接到NFS系统上，这样的话，无论Pod在节点上怎么转移，只要Node跟NFS的对接没问题，数据就可以成功访问。



1)首先要准备nfs的服务器

```shell
#安装nfs服务
sudo yum install nfs-common nfs-kernel-server

#创建需要共享的目录
sudo mkdir  /data/nfs/
sudo chmod a+w /data/nfs/

#修改/etc/exports文件，将需要共享的目录和客户添加进来
/data/nfs/  *(rw,sync) *(ro,rw)

#启动nfs
sudo /etc/init.d/nfs-kernel-server start
```

2）接下来，要在的每个node节点上都安装下nfs 客户端，这样的目的是为了node节点可以驱动nfs设备

```shell
# 在node上安装nfs客户端
yum install nfs-commmon
```

3)接下来，就可以编写pod的配置文件了，创建volume-nfs.yaml

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: volume-nfs
  namespace: dev
spec:
  containers:
  - name: nginx
    image: nginx:1.17.1
    ports:
    - containerPort: 80
    volumeMounts:
    - name: logs-volume
      mountPath: /var/log/nginx
  - name: busybox
    image: busybox:1.30
    command: ["/bin/sh","-c","tail -f /logs/access.log"] 
    volumeMounts:
    - name: logs-volume
      mountPath: /logs
  volumes:
  - name: logs-volume
    nfs:
      server: 192.168.23.101  #nfs服务器地址
      path: /root/data/nfs #共享文件路径
```

4）运行下pod，观察结果

```shell

[root@k8s-master01 ~]# kubectl create -f volume-nfs.yaml
pod/volume-nfs created

[root@k8s-master01 ~]# kubectl get pods volume-nfs -n dev
NAME                  READY   STATUS    RESTARTS   AGE
volume-nfs        2/2     Running   0          2m9s

# 查看nfs服务器上的共享目录，发现已经有文件了
[root@k8s-master01 ~]# ls /root/data/
access.log  error.log
```



## 高级存储

### Volume无法解决的问题

➢ 当某个数据卷不再被挂载使用时，里面的数据如何处理？ 

➢ 如果想要实现只读挂载如何处理？ 

➢ 如果想要只能一个Pod挂载如何处理？ 

➢ 如何只允许某个Pod使用10G的空间？



### PV & PVC

官方文档：https://kubernetes.io/docs/concepts/storage/persistent-volumes/



**PersistentVolume：**简称PV，是由Kubernetes管理员设置的存储，可以配置Ceph、NFS、GlusterFS等常用存储配置，相对于Volume

配置，提供了更多的功能，比如生命周期的管理、大小的限制。PV分为静态和动态。



**PersistentVolumeClaim：**简称PVC，是对存储PV的请求，表示需要什么类型的PV，需要存储的技术人员只需要配置PVC即可使用储，

或者Volume配置PVC的名称即可。



#### PV回收策略

官方文档：https://kubernetes.io/docs/concepts/storage/persistent-volumes/#reclaim-policy

```
➢ Retain：保留，该策略允许手动回收资源，当删除PVC时，PV仍然存在，PV被视为已释放，管理员可以手动回收卷。

➢ Recycle：回收，如果Volume插件支持，Recycle策略会对卷执行rm -rf清理该PV，并使其可用于下一个新的PVC，但是本策略将来会

 		   被弃用，目前只有NFS和HostPath支持该策略。

➢ Delete：删除，如果Volume插件支持，删除PVC时会同时删除PV，动态卷默认为Delete，目前支持Delete的存储后端包括AWS EBS, 					GCE PD, Azure Disk, or OpenStack Cinder等。
```



➢ 可以通过persistentVolumeReclaimPolicy: Recycle字段配置



#### PV访问策略

官方文档：https://kubernetes.io/docs/concepts/storage/persistent-volumes/#access-modes

```
➢ ReadWriteOnce：可以被单节点以读写模式挂载，命令行中可以被缩写为RWO。 

➢ ReadOnlyMany：可以被多个节点以只读模式挂载，命令行中可以被缩写为ROX。 

➢ ReadWriteMany：可以被多个节点以读写模式挂载，命令行中可以被缩写为RWX。 

➢ ReadWriteOncePod ：只允许被单个Pod访问，需要K8s 1.22+以上版本，并且是CSI创建的PV才可使用
```



#### PV的状态

```
➢ Available：可用，没有被PVC绑定的空闲资源。

➢ Bound：已绑定，已经被PVC绑定。 

➢ Released：已释放，PVC被删除，但是资源还未被重新使用。 

➢ Failed：失败，自动回收失败。
```



#### 存储分类

```
➢ 文件存储：一些数据可能需要被多个节点使用，比如用户的头像、用户上传的文件等，实现方式：NFS、NAS、FTP、CephFS等。

➢ 块存储：一些数据只能被一个节点使用，或者是需要将一块裸盘整个挂载使用，比如数据库、Redis等，实现方式：Ceph、GlusterFS、公有          云。

➢ 对象存储：由程序代码直接实现的一种存储方式，云原生应用无状态化常用的实现方式，实现方式：一般是符合S3协议的云存储，比如AWS的S3            存储、Minio、七牛云等。
```



### PV配置示例-NFS/NAS



```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-nfs
spec:
  #容量配置
  capacity: 
    storage: 5Gi
  #卷的模式，目前支持Filesystem（文件系统） 和 Block（块），其中Block类型需要后端存储支持，默认为文件系统
  volumeMode: Filesystem
  #该PV的访问模式
  accessModes: 
  - ReadWriteOnce
  persistentVolumeReclaimPolicy: Recycle  #回收策略
  #PV的类，一个特定类型的PV只能绑定到特定类别的PVC；
  storageClassName: nfs-slow
  #非必须，新版本中已弃用
  mountOptions: 
  - hard
  - nfsvers=4.1
  #NFS服务配置
  nfs:
    #NFS上的共享目录
    path: /data/k8s
    #NFS的IP地址
    server: 192.168.23.101
```



1）NFS服务器安装服务端：

```
 yum install nfs* rpcbind -y
```

2）所有K8s节点安装NFS客户端：

```
yum install nfs-utils -y
```

3）NFS服务端：

```
# mkdir /data/k8s -p

# vim /etc/exports
/data/k8s/ *(rw,sync,no_subtree_check,no_root_squash)

#重新挂载一次
# exportfs -r

#重启
# systemctl restart nfs rpcbind
```

4）挂载测试：

```
mount -t nfs nfs-serverIP:/data/k8s /mnt/
```



### PV配置示例-HostPath



```yaml
kind: PersistentVolume
apiVersion: v1
metadata:
  name: task-pv-volume
  labels:
    type: local
spec:
  storageClassName: hostpath
  capacity:
    storage: 10Gi
  accessModes: 
  - ReadWriteOnce
  hostPath:
    path: "/mnt/data"  #宿主机路径
```



### PV配置示例-CephRBD

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: ceph-rbd-pv
spec:
  capacity:
    storage: 1Gi
  storageClassName: ceph-fast
  accessModes: 
  - ReadWriteOnce
  rbd:
    monitors:
    - 192.168.1.123:6789
    - 192.168.1.124:6789
    - 192.168.1.125:6789
    pool: rbd
    image: ceph-rbd-pv-test
    user: admin
    secretRef:
      name: ceph-secret
    fsType: ext4
    readOnly: false
```

➢ monitors：Ceph的monitor节点的IP

➢ pool：所用Ceph Pool的名称，**可以使用ceph osd pool ls查看**

➢ image：Ceph块设备中的磁盘映像文件，**可以使用rbd create POOL_NAME/IMAGE_NAME --size 1024创建，使用rbd list  POOL_NAME查看**

➢ user：Rados的用户名，默认是admin

➢ secretRef：用于验证Ceph身份的密钥

➢ fsType：文件类型，可以是ext4、XFS等 

➢ readOnly：是否是只读挂载



### 挂载PVC

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: task-pvc-claim
spec:
  accessModes: # 访问模式
  selector: # 采用标签对PV选择
  storageClassName: # 存储类别
  resources: # 请求空间
    requests:
      storage: 5Gi
```



```yaml
kind: Pod
apiVersion: v1
metadata:
  name: task-pv-pod
spec:
  volumes:
  - name: task-pv-storage
  persistentVolumeClaim:
    claimName: task-pvc-claim
  containers:
  - name: task-pv-container
    image: nginx
    ports:
    - containerPort: 80
    name: "http-server"
    volumeMounts:
    - mountPath: "/usr/share/nginx/html"
      name: task-pv-storage
```



### PVC创建和挂载失败的原因



➢ PVC一直Pending的原因：

⚫ PVC的空间申请大小大于PV的大小

⚫ PVC的StorageClassName没有和PV的一致

⚫ PVC的accessModes和PV的不一致



➢ 挂载PVC的Pod一直处于Pending： 

⚫ PVC没有创建成功/PVC不存在

⚫ PVC和Pod不在同一个Namespace