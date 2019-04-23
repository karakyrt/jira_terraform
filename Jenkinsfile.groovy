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
    checkout scm
    stage('Generate Vars') {
        def file = new File("${WORKSPACE}/jira_terraform/jira.tfvars")
        file.write """
        namespace             =  "${namespace}"
        """
      }
    stage("Terraform init") {
      dir("${workspace}/jira_terraform/") {
        sh "terraform init"
      }
    }
    stage("Terraform Apply/Plan"){
      if (!params.terraformDestroy) {
        if (params.terraformApply) {
          dir("${workspace}/jira_terraform/") {
            echo "##### Terraform Applying the Changes ####"
            sh "terraform apply --auto-approve -var-file=jira.tfvars"
        }
      } else {
          dir("${WORKSPACE}/jira_terraform") {
            echo "##### Terraform Plan (Check) the Changes ####"
            sh "terraform plan -var-file=jira.tfvars"
          }
        }
      } 
    }
    stage('Terraform Destroy') {
      if (!params.terraformApply) {
        if (params.terraformDestroy) {
          dir("${WORKSPACE}/jira_terraform") {
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
 }
