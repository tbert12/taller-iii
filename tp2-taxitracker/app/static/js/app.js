'use strict';

var App = angular.module('App', ['ngRoute','ngMaterial','infinite-scroll']);

App.config(function($routeProvider) {
  let views = "/static/view";
  $routeProvider.when('/', {
    controller : 'MainCtrl',
    templateUrl: views + '/main.html',
    //resolve    : { 'guestService': 'guestService' },
  });
  $routeProvider.when('/admin', {
    controller : 'AdminCtrl',
    templateUrl: views + '/admin.html',
  });
  $routeProvider.otherwise({
    redirectTo : '/'
  });
});

App.controller('MainCtrl', function($scope, $rootScope, $log, $http, $routeParams, $location, $route) {
  $rootScope.tittle = "Client";
  $scope.stats = [];
  
  $scope.goToAdmin = function() {
      $location.path('/admin');
  };
  
  $scope.loading = true;
  $scope._end = false;
  $scope.cursor = null;
  $scope.loadStats = function() {
      $scope.loading = true;
      $http.get('/api/stats', { params: { cursor : $scope.cursor } })
          .then(function(res) {
              let data = res.data;
              $scope.stats = $scope.stats.concat(data.stats);
              $scope.cursor = data.cursor
              $scope._end = $scope.cursor != null ? false : true;
              $scope.loading = false;
          }, function(error){
              console.log("ERROR ON LOAD STATS");   
              $scope.loading = false;
          })
  }
  $scope.loadStats();

});

App.controller('AdminCtrl', function($scope, $rootScope, $log, $http, $routeParams, $location, $route) {
  $rootScope.tittle = "Admin";
  
  $scope.goToMain = function() {
      $location.path('/')
  }

  $scope.stats = [];

  $scope.filters = {
    vendorID : null,
    from_date : null,
    to_date : null,
    cursor : null
  };
  $scope._end = false;
  $scope.loading = true;

  $scope.applyFilter = function() {
      $scope.stats = [];
      $scope.loading = true;
      $scope.filters.cursor = null;
      $scope.loadAdminStats();
  }

  $scope.loadAdminStats = function() {
      $scope.loading = true;
      $http.get('/api/admin_stats', { params : $scope.filters })
          .then(function(res) {
              let data = res.data;
              $scope.stats = $scope.stats.concat(data.stats);
              $scope.filters.cursor = data.cursor
              $scope._end = data.cursor === null ? true : false;
              $scope.loading = false;
          }, function(error) {
              $scope.loading = false;
              console.log("Error on load admin stats");
          });
  }

  $scope.loadAdminStats();
});

