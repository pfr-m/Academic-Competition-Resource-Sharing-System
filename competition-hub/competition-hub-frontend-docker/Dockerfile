# 基础镜像：极简版 Nginx
FROM nginx:alpine

# 复制自定义的 Nginx 配置文件覆盖默认配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制打包出来的静态资源到 Nginx 网页根目录
COPY dist /usr/share/nginx/html

# 暴露 80 端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]