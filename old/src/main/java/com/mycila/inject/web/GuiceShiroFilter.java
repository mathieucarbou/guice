/**
 * Copyright (C) 2010 Mycila <mathieu.carbou@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycila.inject.web;

import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.servlet.AbstractShiroFilter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Singleton
public final class GuiceShiroFilter extends AbstractShiroFilter {

    @Inject
    WebSecurityManager securityManager;

    @Override
    public void init() throws Exception {
        super.init();
        setFilterChainResolver(new FilterChainResolver() {
            @Override
            public FilterChain getChain(ServletRequest request, ServletResponse response, FilterChain originalChain) {
                return originalChain;
            }
        });
    }

    @Override
    protected WebSecurityManager createDefaultSecurityManager() {
        return securityManager;
    }

    @Override
    protected void executeChain(final ServletRequest request, final ServletResponse response, final FilterChain origChain) throws IOException, ServletException {
        try {
            HttpContext.change((HttpServletRequest) request, (HttpServletResponse) response);
            super.executeChain(request, response, origChain);
        } finally {
            HttpContext.end();
        }
    }

}
