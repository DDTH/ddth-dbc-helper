package com.github.ddth.dao.jdbc.utils;

import com.github.ddth.dao.utils.DatabaseVendor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementations of {@link IFilter}.
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 1.0.0
 */
public class DefaultFilters {
    /**
     * Base class for {@link IFilter} implementations.
     *
     * @since 1.1.0
     */
    public static abstract class BaseFilter implements IFilter {
        private DatabaseVendor vendor = DatabaseVendor.UNKNOWN;

        protected DatabaseVendor getVendor() {
            return vendor;
        }

        /**
         * Specify the database vendor.
         *
         * @param vendor
         * @return
         */
        public BaseFilter withVendor(DatabaseVendor vendor) {
            this.vendor = vendor;
            return this;
        }
    }

    /**
     * This filter combines two or more filters using an operator.
     */
    public static class FilterOptCombine extends BaseFilter {
        private String operator;
        private List<IFilter> filters = new ArrayList<>();

        public FilterOptCombine() {
        }

        public FilterOptCombine(String operator) {
            withOperator(operator);
        }

        /**
         * @return
         * @since 1.1.0
         */
        protected String getOperator() {
            return operator;
        }

        /**
         * @return
         * @since 1.1.0
         */
        protected List<IFilter> getFilters() {
            return filters;
        }

        /**
         * Set the operator.
         *
         * @param operator
         * @return
         */
        public FilterOptCombine withOperator(String operator) {
            this.operator = operator != null ? operator.trim() : null;
            return this;
        }

        /**
         * Append a filter to the list.
         *
         * @param filter
         * @return
         */
        public FilterOptCombine addFilter(IFilter filter) {
            if (filter != null) {
                this.filters.add(filter);
            }
            return this;
        }

        /**
         * Append filters to the list.
         *
         * @param filters
         * @return
         */
        public FilterOptCombine addFilters(IFilter... filters) {
            if (filters != null) {
                for (IFilter filter : filters) {
                    addFilter(filter);
                }
            }
            return this;
        }

        @Override
        public BuildSqlResult build() {
            if (filters.size() == 0) {
                return new BuildSqlResult(null);
            }
            List<String> clause = new ArrayList<>();
            List<Object> bindValues = new ArrayList<>();

            BuildSqlResult tempResult = filters.get(0).build();
            clause.add("(" + tempResult.clause + ")");
            for (Object obj : tempResult.bindValues) {
                bindValues.add(obj);
            }
            filters.subList(1, filters.size()).forEach(f -> {
                BuildSqlResult temp = f.build();
                clause.add("(" + temp.clause + ")");
                for (Object obj : temp.bindValues) {
                    bindValues.add(obj);
                }
            });
            String opt = " " + operator + " ";
            return new BuildSqlResult(StringUtils.join(clause.toArray(ArrayUtils.EMPTY_STRING_ARRAY), opt),
                    bindValues.toArray());
        }
    }

    /**
     * This filter combines two or more filters using AND clause.
     */
    public static class FilterAnd extends FilterOptCombine {
        public FilterAnd() {
            withOperator("AND");
        }

        /**
         * Set the "and" operator (default value is AND).
         *
         * @param operator
         * @return
         */
        public FilterAnd withOperator(String operator) {
            super.withOperator(StringUtils.isBlank(operator) ? "AND" : operator);
            return this;
        }
    }

    /**
     * This filter combines two or more filters using OR clause.
     */
    public static class FilterOr extends FilterOptCombine {
        public FilterOr() {
            withOperator("OR");
        }

        /**
         * Set the "or" operator (default value is OR).
         *
         * @param operator
         * @return
         */
        public FilterOr withOperator(String operator) {
            super.withOperator(StringUtils.isBlank(operator) ? "OR" : operator);
            return this;
        }
    }

    /**
     * This filter constructs a filter clause {@code <field> <operation> <value>}
     */
    public static class FilterFieldValue extends BaseFilter {
        private final String fieldName, operator;
        private final Object value;

        public FilterFieldValue(String fieldName, String operator, Object value) {
            this.fieldName = fieldName;
            this.operator = operator != null ? operator.trim() : null;
            this.value = value;
        }

        /**
         * @return
         * @since 1.1.0
         */
        protected String getFieldName() {
            return fieldName;
        }

        /**
         * @return
         * @since 1.1.0
         */
        protected String getOperator() {
            return operator;
        }

        /**
         * @return
         * @since 1.1.0
         */
        protected Object getValue() {
            return value;
        }

        @Override
        public BuildSqlResult build() {
            if (value instanceof ParamRawExpression) {
                return new BuildSqlResult(fieldName + " " + operator + " " + ((ParamRawExpression) value).expr);
            } else {
                return new BuildSqlResult(fieldName + " " + operator + " ?", value);
            }
        }
    }

    /**
     * This filter constructs a filter clause {@code <left_value> <operation> <right_value>}
     */
    public static class FilterExpression extends BaseFilter {
        private final String operator;
        private final Object leftValue, rightValue;

        public FilterExpression(Object leftValue, String operator, Object rightValue) {
            this.leftValue = leftValue;
            this.operator = operator != null ? operator.trim() : null;
            this.rightValue = rightValue;
        }

        /**
         * @return
         * @since 1.1.0
         */
        protected String getOperator() {
            return operator;
        }

        /**
         * @return
         * @since 1.1.0
         */
        protected Object getLeftValue() {
            return leftValue;
        }

        /**
         * @return
         * @since 1.1.0
         */
        protected Object getRightValue() {
            return rightValue;
        }

        @Override
        public BuildSqlResult build() {
            StringBuilder clause = new StringBuilder();
            List<Object> bindValues = new ArrayList<>();
            if (leftValue instanceof ParamRawExpression) {
                clause.append(((ParamRawExpression) leftValue).expr);
            } else {
                clause.append("?");
                bindValues.add(leftValue);
            }
            clause.append(" ").append(operator).append(" ");
            if (rightValue instanceof ParamRawExpression) {
                clause.append(((ParamRawExpression) rightValue).expr);
            } else {
                clause.append("?");
                bindValues.add(rightValue);
            }
            return new BuildSqlResult(clause.toString(), bindValues.toArray());
        }
    }
}
