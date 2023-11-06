build-projects:
	cd containers && ./mvnw clean package

build-images:
	cd containers && ./mvnw spring-boot:build-image

install-dependencies: install-messaging-middleware install-database install-data-flow

install-messaging-middleware:
	kubectl create -f kubernetes-config/rabbitmq/

install-database:
	kubectl create -f kubernetes-config/mariadb/

dependency-metrics:
	kubectl create -f kubernetes-config/prometheus/prometheus-clusterroles.yaml
	kubectl create -f kubernetes-config/prometheus/prometheus-clusterrolebinding.yaml
	kubectl create -f kubernetes-config/prometheus/prometheus-serviceaccount.yaml

	kubectl create -f kubernetes-config/prometheus-proxy/

	kubectl create -f kubernetes-config/prometheus/prometheus-configmap.yaml
	kubectl create -f kubernetes-config/prometheus/prometheus-deployment.yaml
	kubectl create -f kubernetes-config/prometheus/prometheus-service.yaml

	kubectl create -f kubernetes-config/grafana/

install-data-flow:
	kubectl create -f kubernetes-config/server/server-roles.yaml
	kubectl create -f kubernetes-config/server/server-rolebinding.yaml
	kubectl create -f kubernetes-config/server/service-account.yaml

	kubectl create -f kubernetes-config/skipper/skipper-config-rabbit.yaml

	kubectl create -f kubernetes-config/skipper/skipper-deployment.yaml
	kubectl create -f kubernetes-config/skipper/skipper-svc.yaml

	kubectl create -f kubernetes-config/server/server-config.yaml

	kubectl create -f kubernetes-config/server/server-svc.yaml
	kubectl create -f kubernetes-config/server/server-deployment.yaml

install-external-services:
	kubectl create -f kubernetes-config/loan-valuation-external-services/loanprovider-svc.yaml
	kubectl create -f kubernetes-config/loan-valuation-external-services/loanprovider-deployment.yaml

install-loan-valuation:
	kubectl create -f kubernetes-config/loan-valuation/loanvaluationsetup-app.yaml

delete-dependencies: delete-data-flow delete-messaging-middleware delete-database

delete-data-flow:
	kubectl delete role scdf-role
	kubectl delete rolebinding scdf-rb
	kubectl delete serviceaccount scdf-sa

	kubectl delete all,cm -l app=scdf-server

	kubectl delete all,cm -l app=skipper

delete-messaging-middleware:
	kubectl delete all -l app=rabbitmq

delete-database:
	kubectl delete all,pvc,secrets -l app=mariadb

