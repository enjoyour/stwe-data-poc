@echo off
chcp 65001 > nul
set APPNAME="stwe-data-poc"
set VERSION="1.0"
set HARBOR_URL="dockerhub.toowe.com:18443"
set LIBRARY_NAME="ai-tools"

set "PULL_CMD=docker pull %HARBOR_URL%/%LIBRARY_NAME%/%APPNAME%:%VERSION%"
set "DEPLOY_CMD=cd /data/azs && docker-compose -f ./docker-compose-app.yml up -d"
set LOCAL_BUILD=false
IF "%1"=="local" (
    set LOCAL_BUILD=true
)

call docker build -t %APPNAME%:%VERSION% -f ../Dockerfile ..

IF %ERRORLEVEL% EQU 0 (
    echo 构建成功，推送到远程仓库
    IF NOT "%LOCAL_BUILD%"=="true" (
        call docker login -u admin https://dockerhub.toowe.com:18443 -p Azs!@#888
        call docker tag  %APPNAME%:%VERSION% %HARBOR_URL%/%LIBRARY_NAME%/%APPNAME%:%VERSION%
        call docker push %HARBOR_URL%/%LIBRARY_NAME%/%APPNAME%:%VERSION%
    ) ELSE (
        echo 仅构建本地image，跳过Docker登录和推送步骤。
    )
) ELSE (
    echo 构建失败，跳过Docker登录和推送步骤。
)



