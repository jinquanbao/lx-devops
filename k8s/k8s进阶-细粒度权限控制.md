## 访问控制概述

Kubernetes作为一个分布式集群的管理工具，保证集群的安全性是其一个重要的任务。所谓的安全性其实就是保证对Kubernetes的各种**客户端**进行**认证和鉴权**操作。

**客户端**

在Kubernetes集群中，客户端通常有两类：

- **User Account**：一般是独立于kubernetes之外的其他服务管理的用户账号。
- **Service Account**：kubernetes管理的账号，用于为Pod中的服务进程在访问Kubernetes时提供身份标识。



![image-20220830105927114](./images/image-20220830105927114.png)

**认证、授权与准入控制**

ApiServer是访问及管理资源对象的唯一入口。任何一个请求访问ApiServer，都要经过下面三个流程：

- Authentication（认证）：身份鉴别，只有正确的账号才能够通过认证
- Authorization（授权）： 判断用户是否有权限对访问的资源执行特定的动作
- Admission Control（准入控制）：用于补充授权机制以实现更加精细的访问控制功能。

![img](.\images\image-20200520103942580.png)



## 授权管理

授权发生在认证成功之后，通过认证就可以知道请求用户是谁， 然后Kubernetes会根据事先定义的授权策略来决定用户是否有权限访问，这个过程就称为授权。

每个发送到ApiServer的请求都带上了用户和资源的信息：比如发送请求的用户、请求的路径、请求的动作等，授权就是根据这些信息和授权策略进行比较，如果符合策略，则认为授权通过，否则会返回错误。

API Server目前支持以下几种授权策略：

- AlwaysDeny：表示拒绝所有请求，一般用于测试
- AlwaysAllow：允许接收所有请求，相当于集群不需要授权流程（Kubernetes默认的策略）
- ABAC：基于属性的访问控制，表示使用用户配置的授权规则对用户请求进行匹配和控制
- Webhook：通过调用外部REST服务对用户进行授权
- Node：是一种专用模式，用于对kubelet发出的请求进行访问控制
- RBAC：基于角色的访问控制（kubeadm安装方式下的默认选项）



## RBAC授权管理

RBAC(Role-Based Access Control) 基于角色的访问控制，主要是在描述一件事情：**给哪些对象授予了哪些权限**

其中涉及到了下面几个概念：

- 对象：User、Groups、ServiceAccount
- 角色：代表着一组定义在资源上的可操作动作(权限)的集合
- 绑定：将定义好的角色跟用户绑定在一起



![img](./images/image-20200519181209566.png)



## 



### RBAC基本概念

RBAC（Role-Based Access Control，基于角色的访问控制）是一种基于企业内个人用户的角色来管理对计算机或网络资源的访问方法，其在Kubernetes 1.5版本中引入，在1.6时升级为Beta版本，并成为Kubeadm安装方式下的默认选项。启用RBAC需要在启动APIServer时指定--authorization-mode=RBAC。

RBAC使用rbac.authorization.k8s.io API组来推动授权决策，允许管理员通过Kubernetes API动态配置策略。



RBAC API声明了4个顶级资源对象：

- Role、ClusterRole：角色，用于指定一组权限
- RoleBinding、ClusterRoleBinding：角色绑定，用于将角色（权限）赋予给对象

管理员可以像使用其他API资源一样使用kubectl API调用这些资源对象。例如：kubectl create -f (resource).yml。



### Role和ClusterRole

Role和ClusterRole的关键区别是，Role是作用于命名空间内的角色，ClusterRole作用于整个集群的角色。

在RBAC API中，Role包含表示一组权限的规则。权限纯粹是附加允许的，没有拒绝规则。

Role只能授权对单个命名空间内的资源的访问权限，比如授权对default命名空间的读取权限：

```yaml
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: default
  name: pod-reader
rules:
- apiGroups: [""] # "" indicates the core API group
  resources: ["pods"]
  verbs: ["get", "watch", "list"]
```

ClusterRole也可将上述权限授予作用于整个集群的Role，主要区别是，ClusterRole是集群范围的，因此它们还可以授予对以下内容的访问权限：

```
集群范围的资源（如Node）。

非资源端点（如/healthz）。

跨所有命名空间的命名空间资源（如Pod）。
```

比如，授予对任何特定命名空间或所有命名空间中的secret的读权限（取决于它的绑定方式）：

```yaml
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  # "namespace" omitted since ClusterRoles are not namespaced
  name: secret-reader
rules:
- apiGroups: [""]
  resources: ["secrets"]
  verbs: ["get", "watch", "list"]
```

### RoleBinding和ClusterRoleBinding

RoleBinding将Role中定义的权限授予User、Group或Service Account。RoleBinding和ClusterRoleBinding最大的区别与Role和ClusterRole的区别类似，即RoleBinding作用于命名空间，ClusterRoleBinding作用于集群。

RoleBinding可以引用同一命名空间的Role进行授权，比如将上述创建的pod-reader的Role授予default命名空间的用户jane，这将允许jane读取default命名空间中的Pod：

```yaml
# This role binding allows "jane" to read pods in the "default" namespace.
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: read-pods
  namespace: default
subjects:
- kind: User
  name: jane # Name is case sensitive
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role #this must be Role or ClusterRole
  name: pod-reader # this must match the name of the Role or ClusterRole you wish to bind to
  apiGroup: rbac.authorization.k8s.io
```

**说明：roleRef：绑定的类别，可以是Role或ClusterRole。**

RoleBinding也可以引用ClusterRole来授予对命名空间资源的某些权限。管理员可以为整个集群定义一组公用的ClusterRole，然后在多个命名空间中重复使用。

比如，创建一个RoleBinding引用ClusterRole，授予dave用户读取development命名空间的Secret：

```yaml
# This role binding allows "dave" to read secrets in the "development" namespace.
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: read-secrets
  namespace: development # This only grants permissions within the "development" namespace.
subjects:
- kind: User
  name: dave # Name is case sensitive
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: secret-reader
  apiGroup: rbac.authorization.k8s.io
```

ClusterRoleBinding可用于在集群级别和所有命名空间中授予权限，比如允许组manager中的所有用户都能读取任何命名空间的Secret：

```yaml
# This cluster role binding allows anyone in the "manager" group to read secrets in any namespace.
kind: ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: read-secrets-global
subjects:
- kind: Group
  name: manager # Name is case sensitive
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: secret-reader
  apiGroup: rbac.authorization.k8s.io
```

### 对集群资源的权限控制

在Kubernetes中，大多数资源都由其名称的字符串表示，例如pods。但是一些Kubernetes API涉及的子资源（下级资源），例如Pod的日志，对应的Endpoint的URL是：

```shell
GET /api/v1/namespaces/{namespace}/pods/{name}/log
```

在这种情况下，pods是命名空间资源，log是Pod的下级资源，如果对其进行访问控制，要使用斜杠来分隔资源和子资源，比如定义一个Role允许读取Pod和Pod日志：

```yaml
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: default
  name: pod-and-pod-logs-reader
rules:
- apiGroups: [""]
  resources: ["pods", "pods/log"]
  verbs: ["get", "list"]
```

针对具体资源（使用resourceNames指定单个具体资源）的某些请求，也可以通过使用get、delete、update、patch等进行授权，比如，只能对一个叫my-configmap的configmap进行get和update操作：

```yaml
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: default
  name: configmap-updater
rules:
- apiGroups: [""]
  resources: ["configmaps"]
  resourceNames: ["my-configmap"]
  verbs: ["update", "get"]
```

**注意：如果使用了resourceNames，则verbs不能是list、watch、create、deletecollection等。**



### 聚合ClusterRole

从Kubernetes 1.9版本开始，Kubernetes可以通过一组ClusterRole创建聚合ClusterRoles，聚合ClusterRoles的权限由控制器管理，并通过匹配ClusterRole的标签自动填充相对应的权限。

比如，匹配rbac.example.com/aggregate-to-monitoring: "true"标签来创建聚合ClusterRole：

```yaml
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: monitoring
aggregationRule:
  clusterRoleSelectors:
  - matchLabels:
      rbac.example.com/aggregate-to-monitoring: "true"
rules: [] # Rules are automatically filled in by the controller manager.
```

然后创建与标签选择器匹配的ClusterRole向聚合ClusterRole添加规则：

```yaml
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: monitoring-endpoints
  labels:
    rbac.example.com/aggregate-to-monitoring: "true"
# These rules will be added to the "monitoring" role.
rules:
- apiGroups: [""]
  resources: ["services", "endpoints", "pods"]
  verbs: ["get", "list", "watch"]
```

### Role示例

以下示例允许读取核心API组中的资源Pods（只写了规则rules部分）：

```yaml
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
```

允许在extensions和apps API组中读写deployments：

```yaml
rules:
- apiGroups: ["extensions", "apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
```

允许对Pods的读和Job的读写：

```yaml
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["batch", "extensions"]
  resources: ["jobs"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
```

允许读取一个名为my-config的ConfigMap（必须绑定到一个RoleBinding来限制到一个命名空间下的ConfigMap）：

```yaml
rules:
- apiGroups: [""]
  resources: ["configmaps"]
  resourceNames: ["my-config"]
  verbs: ["get"]
```

允许读取核心组Node资源（Node属于集群级别的资源，必须放在ClusterRole中，并使用ClusterRoleBinding进行绑定）：

```yaml
rules:
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list", "watch"]
```

允许对非资源端点/healthz和所有其子资源路径的Get和Post请求（必须放在ClusterRole并与ClusterRoleBinding进行绑定）：

```yaml
rules:
- nonResourceURLs: ["/healthz", "/healthz/*"] # '*' in a nonResourceURL is a suffix glob match
  verbs: ["get", "post"]
```

### RoleBinding示例

以下示例绑定为名为“alice@example.com”的用户（只显示subjects部分）：

```yaml
subjects:
- kind: User
  name: "alice@example.com"
  apiGroup: rbac.authorization.k8s.io
```

绑定为名为“frontend-admins”的组：

```yaml
subjects:
- kind: Group
  name: "frontend-admins"
  apiGroup: rbac.authorization.k8s.io
```

绑定为kube-system命名空间中的默认Service Account：

```yaml
subjects:
- kind: ServiceAccount
  name: default
  namespace: kube-system
```

绑定为qa命名空间中的所有Service Account：

```yaml
subjects:
- kind: Group
  name: system:serviceaccounts:qa
  apiGroup: rbac.authorization.k8s.io
```

绑定所有Service Account：

```yaml
subjects:
- kind: Group
  name: system:serviceaccounts
  apiGroup: rbac.authorization.k8s.io
```

绑定所有经过身份验证的用户（v1.5+）：

```yaml
subjects:
- kind: Group
  name: system:authenticated
  apiGroup: rbac.authorization.k8s.io
```

绑定所有未经过身份验证的用户（v1.5+）：

```yaml
subjects:
- kind: Group
  name: system:unauthenticated
  apiGroup: rbac.authorization.k8s.io
```

对于所有用户：

```yaml
subjects:
- kind: Group
  name: system:authenticated
  apiGroup: rbac.authorization.k8s.io
- kind: Group
  name: system:unauthenticated
  apiGroup: rbac.authorization.k8s.io
```

### 命令行的使用

权限的创建可以使用命令行直接创建，较上述方式更加简单、快捷，下面我们逐一介绍常用命令的使用。

#### 1)create role

创建一个Role，命名为pod-reader，允许用户在Pod上执行get、watch和list：

```shell
kubectl create role pod-reader --verb=get --verb=list --verb=watch --resource=pods
```

创建一个指定了resourceNames的Role，命名为pod-reader：

```shell
kubectl create role pod-reader --verb=get --resource=pods --resource-name=readablepod --resource-name=anotherpod
```

创建一个命名为foo，并指定APIGroups的Role：

```shell
kubectl create role foo --verb=get,list,watch --resource=replicasets.apps
```

针对子资源创建一个名为foo的Role：

```shell
kubectl create role foo --verb=get,list,watch --resource=pods,pods/status
```

针对特定/具体资源创建一个名为my-component-lease-holder的Role：

```shell
kubectl create role my-component-lease-holder --verb=get,list,watch,update --resource=lease --resource-name=my-component
```

#### 2)create clusterrole

创建一个名为pod-reader的ClusterRole，允许用户在Pod上执行get、watch和list：

```shell
kubectl create clusterrole pod-reader --verb=get,list,watch --resource=pods
```

创建一个名为pod-reader的ClusterRole，并指定resourceName：

```shell
kubectl create clusterrole pod-reader --verb=get --resource=pods --resource-name=readablepod --resource-name=anotherpod
```

使用指定的apiGroup创建一个名为foo的ClusterRole：

```shell
kubectl create clusterrole foo --verb=get,list,watch --resource=replicasets.apps
```

使用子资源创建一个名为foo的ClusterRole：

```shell
kubectl create clusterrole foo --verb=get,list,watch --resource=pods,pods/status
```

使用non-ResourceURL创建一个名为foo的ClusterRole：

```shell
kubectl create clusterrole "foo" --verb=get --non-resource-url=/logs/*
```

使用指定标签创建名为monitoring的聚合ClusterRole：

```shell
kubectl create clusterrole monitoring --aggregation-rule="rbac.example.com/aggregate-to-monitoring=true"
```

#### 3)create rolebinding

创建一个名为bob-admin-binding的RoleBinding，将名为admin的ClusterRole绑定到名为acme的命名空间中一个名为bob的user：

```shell
kubectl create rolebinding bob-admin-binding --clusterrole=admin --user=bob --namespace=acme
```

创建一个名为myapp-view-binding的RoleBinding，将名为view的ClusterRole，绑定到acme命名空间中名为myapp的ServiceAccount：

```shell
kubectl create rolebinding myapp-view-binding --clusterrole=view --serviceaccount=acme:myapp --namespace=acme
```

#### 4)create clusterrolebinding

创建一个名为root-cluster-admin-binding 的clusterrolebinding，将名为cluster-admin的ClusterRole绑定到名为root的user：

```shell
kubectl create clusterrolebinding root-cluster-admin-binding --clusterrole=cluster-admin --user=root
```

创建一个名为myapp-view-binding的clusterrolebinding，将名为view的ClusterRole绑定到acme命名空间中名为myapp的ServiceAccount：

```shell
kubectl create clusterrolebinding myapp-view-binding --clusterrole=view --serviceaccount=acme:myapp
```

