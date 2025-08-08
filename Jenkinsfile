pipeline {
    agent any

    tools {
        gradle 'Gradle 8.14.3'
    }

    environment {
        REGISTRY = 'image-registry.openshift-image-registry.svc:5000'
        NAMESPACE = 'one-gate-payment'
        APP_NAME = 'payment-service'
        // Update this version when you want to release a new version
        SEMANTIC_VERSION = '1.0.0'
        // SonarQube configuration
        SONARQUBE_URL = 'https://sonarqube.apps.ocp-one-gate-payment.skynux.fun'
    }

    stages {
        stage('Testing'){
            failFast true
            parallel{
                stage('PostgreSQL Integration Test'){
                    steps {
                        script {
                            echo "Running PostgreSQL integration tests..."

                            retry(10) {
                                sh '''
                                    echo "Attempting to connect to PostgreSQL..."

                                    # Direct pipe approach - no command substitution
                                    if curl -v --connect-timeout 5 --max-time 5 telnet://postgresql-service.one-gate-payment.svc.cluster.local:5432 2>&1 | grep -q "Connected to"; then
                                        echo "✅ Successfully connected to PostgreSQL"
                                    else
                                        echo "❌ Connection attempt failed, retrying..."
                                        sleep 5
                                        exit 1
                                    fi
                                '''
                            }
                        }
                    }
                }
                stage('Redis Integration Test'){
                    steps {
                        script {
                            echo "Running Redis integration tests..."
                                retry(10) {
                                    sh '''
                                        echo "Attempting to connect to Redis..."

                                        # Direct pipe approach - no command substitution
                                        if curl -v --connect-timeout 5 --max-time 5 telnet://redis-service.one-gate-payment.svc.cluster.local:6379 2>&1 | grep -q "Connected to"; then
                                            echo "✅ Successfully connected to Redis"
                                        else
                                            echo "❌ Connection attempt failed, retrying..."
                                            sleep 5
                                            exit 1
                                        fi
                                    '''
                                }
                        }
                    }
                }
                stage('Kafka Integration Test'){
                    steps {
                        script {
                            echo "Running Kafka integration tests..."
                                retry(10) {
                                    sh '''
                                        echo "Attempting to connect to Kafka..."

                                        # Direct pipe approach - no command substitution
                                        if curl -v --connect-timeout 5 --max-time 5 telnet://one-gate-payment-kafka-kafka-bootstrap.one-gate-payment.svc.cluster.local:9092 2>&1 | grep -q "Connected to"; then
                                            echo "✅ Successfully connected to Kafka"
                                        else
                                            echo "❌ Connection attempt failed, retrying..."
                                            sleep 5
                                            exit 1
                                        fi
                                    '''
                                }
                        }
                    }
                }
                stage('Unit Test & SAST Analysis') {
                    steps {
                        withCredentials([string(credentialsId: 'sonarqube-payment-service-token', variable: 'SONAR_TOKEN')]) {
                            script {
                                echo "Running SAST analysis with SonarQube..."

                                sh """
                                    gradle clean test jacocoTestReport sonar \\
                                        -Dsonar.projectKey=payment-service \\
                                        -Dsonar.projectName='payment-service' \\
                                        -Dsonar.host.url=\${SONARQUBE_URL} \\
                                        -Dsonar.token=\${SONAR_TOKEN} \\
                                        -Dsonar.junit.reportPaths=build/test-results/test \\
                                        -Dsonar.junit.reportPaths=build/test-results/test \\
                                        -Dsonar.coverage.exclusions="**/domains/dtos/**,**/domains/entities/**,
                                        **/domains/producers/**,
                                        **/applications/resolvers/**,**/commons/configs/**,
                                        **/commons/exceptions/**,**/commons/errorhandlers/**,
                                        **/*Dto.kt,**/*Config.kt,**/*Request.kt,
                                        **/*Response.kt,**/*Req.kt,**/*Resp.kt,
                                        **/ShopeePayRepositoryImpl.kt,
                                        **/GopayRepositoryImpl.kt" \\
                                        -Dsonar.exclusions="**/commons/configs/**" \\
                                        --no-daemon \\
                                        --console=plain \\
                                        --quiet

                                    echo "✅ SAST analysis completed"
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Build with OpenShift BuildConfig') {
            steps {
                script {
                    echo "Triggering OpenShift build for ${APP_NAME}:${SEMANTIC_VERSION}"

                    sh """
                        # Switch to the correct project
                        oc project ${NAMESPACE}

                        # Create or update BuildConfig if needed
                        oc apply -f - <<EOF
apiVersion: build.openshift.io/v1
kind: BuildConfig
metadata:
  name: ${APP_NAME}
  namespace: ${NAMESPACE}
  labels:
    app: ${APP_NAME}
spec:
  source:
    type: Git
    git:
      uri: git@github.com:cisnux-seed/payment-service.git
      ref: main
    sourceSecret:
      name: github-ssh-keys
  strategy:
    type: Docker
    dockerStrategy:
      dockerfilePath: Dockerfile
  output:
    to:
      kind: ImageStreamTag
      name: ${APP_NAME}:latest
  triggers:
  - type: Manual
  runPolicy: Serial
EOF

                        # Create or update ImageStream
                        oc apply -f - <<EOF
apiVersion: image.openshift.io/v1
kind: ImageStream
metadata:
  name: ${APP_NAME}
  namespace: ${NAMESPACE}
  labels:
    app: ${APP_NAME}
spec:
  lookupPolicy:
    local: false
EOF

                        # Start the build
                        echo "Starting OpenShift build..."
                        oc start-build ${APP_NAME} --wait --follow | cat

                        # Tag the built image with semantic version
                        oc tag ${APP_NAME}:latest ${APP_NAME}:${SEMANTIC_VERSION}

                        echo "✅ Build completed with tags: latest and ${SEMANTIC_VERSION}"
                    """
                }
            }
        }

        stage('Apply OpenShift Resources') {
            steps {
                script {
                    echo "Applying OpenShift resources from repository..."

                    sh """
                        oc project ${NAMESPACE}

                        # Apply the YAML files from the openshift folder in the repo
                        oc apply -f openshift/secrets.yaml
                        oc apply -f openshift/service.yaml
                        oc apply -f openshift/deployment.yaml
                        oc apply -f openshift/hpa.yaml

                        echo "✅ OpenShift resources applied successfully"
                    """
                }
            }
        }

        stage('Deploy Application') {
            steps {
                script {
                    echo "Deploying application with latest image..."

                    sh """
                        oc project ${NAMESPACE}

                        # Force update the deployment to pull the new image
                        oc set image deployment/${APP_NAME} ${APP_NAME}=${REGISTRY}/${NAMESPACE}/${APP_NAME}:latest -n ${NAMESPACE}

                        # Restart the deployment to ensure new image is pulled
                        oc rollout restart deployment/${APP_NAME} -n ${NAMESPACE}

                        # Wait for rollout to complete
                        oc rollout status deployment/${APP_NAME} -n ${NAMESPACE} --timeout=300s | cat

                        # Show deployment status
                        oc get pods -l app=${APP_NAME} -n ${NAMESPACE}

                        # Show the image being used
                        echo "Current deployment image:"
                        oc get deployment ${APP_NAME} -o jsonpath='{.spec.template.spec.containers[0].image}' -n ${NAMESPACE}
                        echo ""

                        echo "✅ Deployed with tags: latest and ${SEMANTIC_VERSION}"
                        echo "Images available in registry:"
                        oc get imagestream ${APP_NAME} -o jsonpath='{.status.tags[*].tag}' | tr ' ' '\\n'
                    """
                }
            }
        }
    }

    post {
        success {
            echo "🎉 Pipeline completed successfully!"
            echo "Application deployed using OpenShift BuildConfig"
            echo "Images available:"
            echo "  - ${APP_NAME}:latest"
            echo "  - ${APP_NAME}:${SEMANTIC_VERSION}"
            echo ""
            echo "To access the service via port-forward:"
            echo "oc port-forward svc/${APP_NAME} 8080:8080 -n ${NAMESPACE}"
        }
        failure {
            script {
                echo "❌ Pipeline failed! Check the logs above for details."
                // Show build logs if build failed
                sh """
                    echo "=== Recent Build Logs ==="
                    oc logs -l build=${APP_NAME} --tail=50 -n ${NAMESPACE} || true
                """
            }
        }
    }
}