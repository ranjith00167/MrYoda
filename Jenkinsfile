pipeline {
    agent any

    options {
        timestamps()
    }

    stages {

        stage('Info') {
            steps {
                echo "Branch: ${env.BRANCH_NAME}"
            }
        }

        stage('Build & Test (STAGING)') {
            when {
                branch 'staging'
            }
            steps {
                echo 'Running full test suite for STAGING'
                sh 'mvn clean test'
            }
        }

        stage('Skip DEV') {
            when {
                branch 'dev'
            }
            steps {
                echo 'DEV branch detected – skipping heavy execution'
            }
        }
    }

    post {
        always {
            echo 'Pipeline completed'
        }
        failure {
            echo 'Build failed – check Surefire/TestNG reports'
        }
    }
}
