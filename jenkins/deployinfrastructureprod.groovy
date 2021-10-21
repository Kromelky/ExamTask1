pipeline {

    agent any

    environment {
        registryCredentials = "nexus-jenkins-acc"
        registry = "10.0.0.179:8085/"
        repo = "https://github.com/Kromelky/AndersenExamApp2"
        imageName = 'kromelky/application1'
        gitHubAuthId = 'git-kromelky-token'
        nexus_login = "nexus-acc"
        application_label = "1"
    }

    stages {
        stage('Init terraform') {
            steps {
                dir("terraform/prod"){
                    sh "terraform init"
                }
            }
        }

        stage('Plan terraform') {
            steps {
                dir("terraform/prod"){
                    withCredentials([usernamePassword(credentialsId: registryCredentials, passwordVariable: 'C_PASS', usernameVariable: 'C_USER')]) {
                        try {
                            sh """
                            terraform plan -var-file="tfvars/prod.tfvars" -var "docker_pass=${C_PASS}" -var "docker_login=${C_USER}" -var "imageName=${imageName}" -var "instance_label=${application_label}"
                            """
                        }
                        catch (Exception ex)
                        {
                                sh """
                                terraform init -migrate-state
                                terraform plan -var-file="tfvars/prod.tfvars" -var "docker_pass=${C_PASS}" -var "docker_login=${C_USER}" -var "imageName=${imageName}" -var "instance_label=${application_label}"
                                """
                        }
                    }
                }
            }
        }

        stage('Apply terraform') {
            steps {
                dir("terraform/prod"){
                    withCredentials([usernamePassword(credentialsId: registryCredentials, passwordVariable: 'C_PASS', usernameVariable: 'C_USER')]) {
                        sh """
                         terraform apply -var-file="tfvars/prod.tfvars" -var "docker_pass=${C_PASS}" -var "docker_login=${C_USER}" -var "imageName=${imageName}"  -var "instance_label=${application_label}" -auto-approve
                         """
                    }
                }
            }
        }
    }
}