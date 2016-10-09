#!groovy

def checkoutCode() {
    stage 'checkout'
    checkout scm: [$class: 'GitSCM', branches: [[name: '*/master']], userRemoteConfigs: [[url: 'https://github.com/codesqueak/Nascom.git']]]
}

def build() {
    stage 'build'
    sh './gradlew clean build -x test'
}

def test() {
    stage 'test'
    sh './gradlew test'
}

def junitreport() {
    stage 'JUnit report'
    step([$class: 'JUnitResultArchiver', testResults: 'build/test-results/test/TEST-*.xml'])
}

def findbugsreport() {
    stage 'Findbugs report'
    step([$class: 'FindBugsPublisher', pattern: 'build/reports/findbugs/main.xml'])
}

def jacocoreport() {
    stage 'Jacoco report'
    step([$class: 'JacocoPublisher', execPattern: 'build/jacoco/jacocoTest.exec', pattern: 'build/jacoco/classpathdumps/net/sleepymouse/emulator/**/*.class'])
}

stage 'execute In Memory Record Store build'

node {
    checkoutCode()
    build()
    test()
    junitreport()
    findbugsreport()
    jacocoreport()
}
