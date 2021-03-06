import './components/initComponents.js';
import { exoConstants } from '../js/eXoConstants.js';
import * as calendarDirectives from './calendarDirectives.js';

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';

// should expose the locale ressources as REST API
const url = `${exoConstants.PORTAL}/${exoConstants.PORTAL_REST}/i18n/bundle/locale.portlet.calendar.calendar-${lang}.json`;

Vue.directive('exo-tooltip', calendarDirectives.tooltip);

// get overrided components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('HomePageCalendarPortlet');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

// getting locale ressources
export function init() {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale ressources are ready
    new Vue({
      el: '#calendarHomeApp',
      template: '<exo-home-calendar></exo-home-calendar>',
      i18n
    });
  });
}