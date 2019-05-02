#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper

node('master') {
  properties([parameters([
    booleanParam(defaultValue: false, description: 'Apply All Changes', name: 'terraformApply'),
    booleanParam(defaultValue: false, description: 'Destroy All', name: 'terraformDestroy'),
    string(defaultValue: 'test', description: 'Please provide namespace for jira-deployment', name: 'namespace', trim: true)
    ]
    )])

    stage('Checkout SCM') {
      git  'https://github.com/fuchicorp/terraform.git'
    }

    stage('Generate Vars') {
        def file = new File("${WORKSPACE}/google_jira/jira.tfvars")
        file.write """
        namespace    =  "${params.namespace}"
        """
    }

    stage("Terraform init") {
      dir("${WORKSPACE}/google_jira/") {
        sh "terraform init"
      }

    }

    stage("Terraform Apply/Plan"){
      if (!params.terraformDestroy) {
        if (params.terraformApply) {
          dir("${WORKSPACE}/google_jira/") {
            echo "##### Terraform Applying the Changes ####"
            sh "terraform apply --auto-approve -var-file=jira.tfvars"
          }

      } else {
            dir("${WORKSPACE}/google_jira/") {
              echo "##### Terraform Plan (Check) the Changes ####"
              sh "terraform plan -var-file=jira.tfvars"
            }
          }

      }
    }
    stage('Terraform Destroy') {
      if (!params.terraformApply) {
        if (params.terraformDestroy) {
          dir("${WORKSPACE}/google_jira/") {
            echo "##### Terraform Destroying ####"
            sh "terraform destroy --auto-approve -var-file=jira.tfvars"
          }
        }
      }
    }
       if (params.terraformDestroy) {
         if (params.terraformApply) {
           println("""
           Sorry you can not destroy and apply at the same time
           """)
        }
    }

    stage("Sending slack notification") {
      slackSend baseUrl: 'https://fuchicorp.slack.com/services/hooks/jenkins-ci/', channel: 'test-message', color: 'green', message: 'Jira job build successfull', tokenCredentialId: 'slack-token'
    }
 }