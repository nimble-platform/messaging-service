node ('nimble-jenkins-slave') {
    stage('Download Latest') {
        git(url: 'https://github.com/nimble-platform/messaging-service.git', branch: 'master')
    }

    stage ('Build docker image') {
        sh 'mvn -Dmaven.test.skip=true clean install'
        sh 'docker build -t nimbleplatform/messaging-service .'
    }

    stage ('Push docker image') {
        withDockerRegistry([credentialsId: 'NimbleDocker']) {
            sh 'docker push nimbleplatform/messaging-service'
        }
    }

    stage ('Deploy') {
        sh 'kubectl apply -f kubernetes/deploy.yaml -n prod --validate=false'
    }

    stage ('Print-deploy logs') {
        sh 'sleep 60'
        sh 'kubectl  -n prod logs deploy/object-storage-app -c object-storage-app'
    }
}